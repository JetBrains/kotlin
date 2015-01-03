/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.preloading;

import org.jetbrains.kotlin.preloading.instrumentation.Instrumenter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Preloader {

    public static final int PRELOADER_ARG_COUNT = 4;
    private static final String INSTRUMENT_PREFIX = "instrument=";

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

        String modeStr = args[3];
        final Mode mode = parseMode(modeStr);
        URL[] instrumentersClasspath = parseInstrumentersClasspath(mode, modeStr);

        final long startTime = System.nanoTime();

        ClassLoader parent = Preloader.class.getClassLoader();

        ClassLoader withInstrumenter = instrumentersClasspath.length > 0 ? new URLClassLoader(instrumentersClasspath, parent) : parent;

        final Handler handler = getHandler(mode, withInstrumenter);
        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(files, classNumber, withInstrumenter, null, handler);

        Class<?> mainClass = preloaded.loadClass(mainClassCanonicalName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Runtime.getRuntime().addShutdownHook(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mode != Mode.NO_TIME) {
                            System.out.println();
                            System.out.println("=== Preloader's measurements: ");
                            long dt = System.nanoTime() - startTime;
                            System.out.format("Total time: %.3fs\n", dt / 1e9);
                        }
                        handler.done();
                    }
                })
        );

        mainMethod.invoke(0, new Object[] {Arrays.copyOfRange(args, PRELOADER_ARG_COUNT, args.length)});
    }

    private static URL[] parseInstrumentersClasspath(Mode mode, String modeStr)
            throws MalformedURLException {
        URL[] instrumentersClasspath;
        if (mode == Mode.INSTRUMENT) {
            List<File> instrumentersClassPathFiles = parseClassPath(getClassPath(modeStr));
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

    private static String getClassPath(String modeStr) {
        return modeStr.substring(INSTRUMENT_PREFIX.length());
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

    private static Handler getHandler(Mode mode, ClassLoader withInstrumenter) {
        if (mode == Mode.NO_TIME) return new Handler();

        final Instrumenter instrumenter = mode == Mode.INSTRUMENT ? loadInstrumenter(withInstrumenter) : Instrumenter.DO_NOTHING;

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
                System.out.println();
                System.out.println("Loaded classes: " + counter[0]);
                System.out.println("Loaded classes size: " + size[0]);
                System.out.println();

                instrumenter.dump(System.out);
            }

            @Override
            public byte[] instrument(String resourceName, byte[] data) {
                return instrumenter.instrument(resourceName, data);
            }
        };
    }

    private static Instrumenter loadInstrumenter(ClassLoader withInstrumenter) {
        ServiceLoader<Instrumenter> loader = ServiceLoader.load(Instrumenter.class, withInstrumenter);
        Iterator<Instrumenter> instrumenters = loader.iterator();
        if (instrumenters.hasNext()) {
            Instrumenter instrumenter = instrumenters.next();
            if (instrumenters.hasNext()) {
                System.err.println("PRELOADER WARNING: Only the first instrumenter is used: " + instrumenter.getClass());
            }
            return instrumenter;
        }
        else {
            System.err.println("PRELOADER WARNING: No instrumenters found");
            return Instrumenter.DO_NOTHING;
        }
    }

    private enum Mode {
        NO_TIME,
        TIME,
        INSTRUMENT
    }

    private static Mode parseMode(String arg) {
        if ("time".equals(arg)) return Mode.TIME;
        if ("notime".equals(arg)) return Mode.NO_TIME;
        if (arg.startsWith(INSTRUMENT_PREFIX)) return Mode.INSTRUMENT;

        System.out.println("Unrecognized argument: " + arg);
        printUsageAndExit();
        return Mode.NO_TIME;
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: Preloader <paths to jars> <main class> <class number estimate> <notime|time|instrument=<instrumenters class path>> <parameters to pass to the main class>");
        System.exit(1);
    }

    private static class Handler extends ClassHandler {
        public void done() {}
    }
}
