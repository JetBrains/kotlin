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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Preloader {
    public static final int DEFAULT_CLASS_NUMBER_ESTIMATE = 4096;

    public static void main(String[] args) throws Exception {
        String javaVersion = System.getProperty("java.specification.version");
        if (javaVersion.equals("1.6") || javaVersion.equals("1.7")) {
            System.err.println("error: running the Kotlin compiler under Java " + javaVersion + " is not supported. " +
                               "Java 1.8 or later is required");
            System.exit(1);
        }

        try {
            run(args);
        }
        catch (PreloaderException e) {
            System.err.println("error: " + e.toString());
            System.err.println();
            printUsage(System.err);
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        final long startTime = System.nanoTime();

        final Options options = parseOptions(args);

        ClassLoader classLoader = createClassLoader(options);

        final Handler handler = getHandler(options, classLoader);
        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(options.classpath, options.estimate, classLoader, null, handler);

        Class<?> mainClass = preloaded.loadClass(options.mainClass);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Runtime.getRuntime().addShutdownHook(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (options.measure) {
                            System.out.println();
                            System.out.println("=== Preloader's measurements: ");
                            System.out.format("Total time: %.3fs\n", (System.nanoTime() - startTime) / 1e9);
                        }
                        handler.done();
                    }
                })
        );

        //noinspection SSBasedInspection
        mainMethod.invoke(0, (Object) options.arguments.toArray(new String[options.arguments.size()]));
    }

    private static ClassLoader createClassLoader(Options options) throws MalformedURLException {
        ClassLoader parent = Preloader.class.getClassLoader();

        List<File> instrumenters = options.instrumenters;
        if (options.arguments.contains("-Xuse-javac")) {
            File toolsJar = getJdkToolsJar();
            if (toolsJar != null) {
                instrumenters.add(toolsJar);
            }
        }

        if (instrumenters.isEmpty()) return parent;

        URL[] classpath = new URL[instrumenters.size()];
        for (int i = 0; i < instrumenters.size(); i++) {
            classpath[i] = instrumenters.get(i).toURI().toURL();
        }

        return new URLClassLoader(classpath, parent);
    }

    private static File getJdkToolsJar() {
        try {
            String javaHomePath = System.getProperty("java.home");
            if (javaHomePath == null || javaHomePath.isEmpty()) {
                return null;
            }
            File javaHome = new File(javaHomePath);
            File toolsJar = new File(javaHome, "lib/tools.jar");
            if (toolsJar.exists()) {
                return toolsJar.getCanonicalFile();
            }

            // We might be inside jre.
            if (javaHome.getName().equals("jre")) {
                toolsJar = new File(javaHome.getParent(), "lib/tools.jar");
                if (toolsJar.exists()) {
                    return toolsJar.getCanonicalFile();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    private static Options parseOptions(String[] args) throws Exception {
        List<File> classpath = Collections.emptyList();
        boolean measure = false;
        List<File> instrumenters = new ArrayList<File>();
        int estimate = DEFAULT_CLASS_NUMBER_ESTIMATE;
        String mainClass = null;
        List<String> arguments = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            boolean end = i == args.length - 1;

            if ("-help".equals(arg) || "-h".equals(arg)) {
                printUsage(System.out);
                System.exit(0);
            }
            else if ("-cp".equals(arg) || "-classpath".equals(arg)) {
                if (end) throw new PreloaderException("no argument provided to " + arg);
                classpath = parseClassPath(args[++i]);
            }
            else if ("-estimate".equals(arg)) {
                if (end) throw new PreloaderException("no argument provided to " + arg);
                estimate = Integer.parseInt(args[++i]);
            }
            else if ("-instrument".equals(arg)) {
                if (end) throw new PreloaderException("no argument provided to " + arg);
                instrumenters = parseClassPath(args[++i]);
            }
            else if ("-measure".equals(arg)) {
                measure = true;
            }
            else {
                mainClass = arg;
                arguments.addAll(Arrays.asList(args).subList(i + 1, args.length));
                break;
            }
        }

        if (mainClass == null) throw new PreloaderException("no main class name provided");

        return new Options(classpath, measure, instrumenters, estimate, mainClass, arguments);
    }

    private static List<File> parseClassPath(String classpath) {
        String[] paths = classpath.split(File.pathSeparator);
        List<File> files = new ArrayList<File>(paths.length);
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                throw new PreloaderException("file does not exist: " + file);
            }
            files.add(file);
        }
        return files;
    }

    private static Handler getHandler(Options options, ClassLoader withInstrumenter) {
        if (!options.measure) return new Handler();

        final Instrumenter instrumenter = options.instrumenters.isEmpty() ? Instrumenter.DO_NOTHING : loadInstrumenter(withInstrumenter);

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

    private static void printUsage(PrintStream out) {
        out.println("usage: java -jar kotlin-preloader.jar [<preloader-options>] <main-class> [<main-class-arguments>]");
        out.println("where possible options include:");
        out.println("  -classpath (-cp) <paths>    Paths where to find class files");
        out.println("  -measure                    Record and output the total time taken by the program and number of loaded classes");
        out.println("  -instrument <paths>         Paths where the instrumenter will be looked up by java.util.ServiceLoader");
        out.println("                              (the class must implement " + Instrumenter.class.getCanonicalName() + " interface)");
        out.println("  -estimate <number>          Class number estimate (" + DEFAULT_CLASS_NUMBER_ESTIMATE + " by default)");
        out.println("  -help (-h)                  Output this help message");
    }

    private static class Options {
        public final List<File> classpath;
        public final boolean measure;
        public final List<File> instrumenters;
        public final int estimate;
        public final String mainClass;
        public final List<String> arguments;

        private Options(
                List<File> classpath,
                boolean measure,
                List<File> instrumenters,
                int estimate,
                String mainClass,
                List<String> arguments
        ) {
            this.classpath = classpath;
            this.measure = measure;
            this.instrumenters = instrumenters;
            this.estimate = estimate;
            this.mainClass = mainClass;
            this.arguments = arguments;
        }
    }

    public static class PreloaderException extends RuntimeException {
        public PreloaderException(String message) {
            super(message);
        }

        public PreloaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class Handler extends ClassHandler {
        public void done() {}
    }
}
