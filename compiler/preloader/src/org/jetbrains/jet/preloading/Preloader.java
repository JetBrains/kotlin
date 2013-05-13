package org.jetbrains.jet.preloading;

import org.jetbrains.jet.preloading.instrumentation.Instrumenter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Preloader {

    public static final int PRELOADER_ARG_COUNT = 4;
    private static final String PROFILE = "profile=";

    public static void main(String[] args) throws Exception {
        if (args.length < PRELOADER_ARG_COUNT) {
            printUsageAndExit();
        }

        List<File> files = parseClassPath(args[0]);

        String mainClassCanonicalName = args[1];

        int classNumber;
        try {
            classNumber = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            printUsageAndExit();
            return;
        }

        String profilingModeStr = args[3];
        ProfilingMode profilingMode = parseMeasureTime(profilingModeStr);
        URL[] instrumentersClasspath = parseInstrumentersClasspath(profilingMode, profilingModeStr);

        long startTime = System.nanoTime();

        ClassLoader parent = Preloader.class.getClassLoader();

        ClassLoader withInstrumenter = instrumentersClasspath.length > 0 ? new URLClassLoader(instrumentersClasspath, parent) : parent;

        Handler handler = getHandler(profilingMode, withInstrumenter);
        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(files, classNumber, withInstrumenter, handler);

        Class<?> mainClass = preloaded.loadClass(mainClassCanonicalName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        try {
            mainMethod.invoke(0, new Object[] {Arrays.copyOfRange(args, PRELOADER_ARG_COUNT, args.length)});
        }
        finally {
            if (profilingMode != ProfilingMode.NO_TIME) {
                long dt = System.nanoTime() - startTime;
                System.out.format("Total time: %.3fs\n", dt / 1e9);
            }
            handler.done();
        }
    }

    private static URL[] parseInstrumentersClasspath(ProfilingMode profilingMode, String profilingModeStr)
            throws MalformedURLException {
        URL[] instrumentersClasspath;
        if (profilingMode == ProfilingMode.PROFILE) {
            List<File> instrumentersClassPathFiles = parseClassPath(getClassPath(profilingModeStr));
            instrumentersClasspath = new URL[instrumentersClassPathFiles.size()];
            for (int i = 0; i < instrumentersClassPathFiles.size(); i++) {
                File file = instrumentersClassPathFiles.get(i);
                instrumentersClasspath[i] = file.toURI().toURL();
            }
        }
        else {
            instrumentersClasspath = new URL[0];
        }
        return instrumentersClasspath;
    }

    private static String getClassPath(String profilingModeStr) {
        return profilingModeStr.substring(PROFILE.length());
    }

    private static List<File> parseClassPath(String classpath) {
        String[] paths = classpath.split("\\" + File.pathSeparator);
        List<File> files = new ArrayList<File>(paths.length);
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("File does not exist: " + file);
                printUsageAndExit();
            }
            files.add(file);
        }
        return files;
    }

    private static Handler getHandler(ProfilingMode profilingMode, ClassLoader withInstrumenter) {
        if (profilingMode == ProfilingMode.NO_TIME) return new Handler();

        ServiceLoader<Instrumenter> loader = ServiceLoader.load(Instrumenter.class, withInstrumenter);
        Iterator<Instrumenter> instrumenters = loader.iterator();
        final Instrumenter instrumenter = instrumenters.hasNext() ? instrumenters.next() : Instrumenter.DO_NOTHING;

        final int[] counter = new int[1];
        final int[] size = new int[1];
        return new Handler() {
            @Override
            public void beforeDefineClass(String name, int sizeInBytes) {
                counter[0]++;
                size[0] += sizeInBytes;
            }

            @Override
            public void done() {
                System.out.println("Loaded classes: " + counter[0]);
                System.out.println("Loaded classes size: " + size[0]);

                instrumenter.dump(System.out);
            }

            @Override
            public byte[] instrument(String resourceName, byte[] data) {
                return instrumenter.instrument(resourceName, data);
            }
        };
    }

    private enum ProfilingMode {
        NO_TIME,
        TIME,
        PROFILE
    }

    private static ProfilingMode parseMeasureTime(String arg) {
        if ("time".equals(arg)) return ProfilingMode.TIME;
        if ("notime".equals(arg)) return ProfilingMode.NO_TIME;
        if (arg.startsWith(PROFILE)) return ProfilingMode.PROFILE;

        System.out.println("Unrecognized argument: " + arg);
        printUsageAndExit();
        return ProfilingMode.NO_TIME;
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: Preloader <paths to jars> <main class> <class number estimate> <time|notime|profile=<profiling class path>> <parameters to pass to the main class>");
        System.exit(1);
    }

    private static class Handler extends ClassPreloadingUtils.ClassHandler {
        public void done() {}
    }
}
