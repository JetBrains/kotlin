package org.jetbrains.jet.preloading;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Preloader {

    public static final int PRELOADER_ARG_COUNT = 4;

    public static void main(String[] args) throws Exception {
        if (args.length < PRELOADER_ARG_COUNT) {
            printUsageAndExit();
        }

        String classpath = args[0];
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

        boolean printTime = parseMeasureTime(args[3]);
        long startTime = System.nanoTime();

        ClassLoader parent = Preloader.class.getClassLoader();

        final int[] counter = new int[1];
        final int[] size = new int[1];
        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(files, classNumber, parent,
                                                                    !printTime ? null :
                                                                    new ClassPreloadingUtils.ClassHandler() {
                                                                        @Override
                                                                        public void beforeDefineClass(String name, int sizeInBytes) {
                                                                            counter[0]++;
                                                                            size[0] += sizeInBytes;
                                                                        }
                                                                    });

        Class<?> mainClass = preloaded.loadClass(mainClassCanonicalName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        try {
            mainMethod.invoke(0, new Object[] {Arrays.copyOfRange(args, PRELOADER_ARG_COUNT, args.length)});
        }
        finally {
            if (printTime) {
                long dt = System.nanoTime() - startTime;
                System.out.format("Total time: %.3fs\n", dt / 1e9);
                System.out.println("Loaded classes: " + counter[0]);
                System.out.println("Loaded classes size: " + size[0]);
            }
        }
    }

    private static boolean parseMeasureTime(String arg) {
        if ("time".equals(arg)) return true;
        if ("notime".equals(arg)) return true;
        System.out.println("Unrecognized argument: " + arg);
        printUsageAndExit();
        return false;
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: Preloader <paths to jars> <main class> <class number estimate> <time|notime> <parameters to pass to the main class>");
        System.exit(1);
    }
}
