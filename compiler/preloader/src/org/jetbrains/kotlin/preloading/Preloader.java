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
            System.err.println("error: number expected: " + e.getMessage());
            printUsageAndExit();
            return;
        }

        final Mode mode = parseMode(args[3]);

        final long startTime = System.nanoTime();

        ClassLoader parent = Preloader.class.getClassLoader();

        ClassLoader withInstrumenter =
            mode instanceof Mode.Instrument ? new URLClassLoader(((Mode.Instrument) mode).classpath, parent) : parent;

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

    private static List<File> parseClassPath(String classpath) {
        String[] paths = classpath.split("\\" + File.pathSeparator);
        List<File> files = new ArrayList<File>(paths.length);
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("error: file does not exist: " + file);
                printUsageAndExit();
            }
            files.add(file);
        }
        return files;
    }

    private static Handler getHandler(Mode mode, ClassLoader withInstrumenter) {
        if (mode == Mode.NO_TIME) return new Handler();

        final Instrumenter instrumenter = mode instanceof Mode.Instrument ? loadInstrumenter(withInstrumenter) : Instrumenter.DO_NOTHING;

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
                System.err.println("warning: only the first preloader instrumenter is used: " + instrumenter.getClass());
            }
            return instrumenter;
        }
        else {
            System.err.println("warning: no preloader instrumenters found");
            return Instrumenter.DO_NOTHING;
        }
    }

    private interface Mode {
        Mode NO_TIME = new Mode() {};
        Mode TIME = new Mode() {};

        class Instrument implements Mode {
            public final URL[] classpath;

            Instrument(URL[] classpath) {
                this.classpath = classpath;
            }
        }
    }

    private static Mode parseMode(String arg) {
        if ("time".equals(arg)) return Mode.TIME;
        if ("notime".equals(arg)) return Mode.NO_TIME;

        if (arg.startsWith(INSTRUMENT_PREFIX)) {
            List<File> files = parseClassPath(arg.substring(INSTRUMENT_PREFIX.length()));
            URL[] classpath = new URL[files.size()];
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                try {
                    classpath[i] = file.toURI().toURL();
                }
                catch (MalformedURLException e) {
                    System.err.println("error: malformed URL: " + e.getMessage());
                    printUsageAndExit();
                }
            }
            return new Mode.Instrument(classpath);
        }

        System.err.println("error: unrecognized argument: " + arg);
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
