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

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Preloader {
    public static final int DEFAULT_CLASS_NUMBER_ESTIMATE = 4096;

    public static void main(String[] args) throws Exception {
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
        Options options = parseOptions(args);

        ClassLoader classLoader = new URLClassLoader(toUrls(options.classpath), Preloader.class.getClassLoader());

        Class<?> mainClass = classLoader.loadClass(options.mainClass);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        Thread.currentThread().setContextClassLoader(classLoader);
        String classPathString = options.classpath.stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator));
        String savedClasspathProperty = System.setProperty("java.class.path", classPathString);

        try {
            mainMethod.invoke(0, (Object) options.arguments.toArray(new String[0]));
        }
        finally {
            if (savedClasspathProperty == null) System.clearProperty("java.class.path");
            else System.setProperty("java.class.path", savedClasspathProperty);
        }
    }

    private static URL[] toUrls(List<File> classpath) throws MalformedURLException {
        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = classpath.get(i).toURI().toURL();
        }
        return urls;
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    private static Options parseOptions(String[] args) {
        List<File> classpath = Collections.emptyList();
        String mainClass = null;
        List<String> arguments = new ArrayList<>();

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
            else {
                mainClass = arg;
                arguments.addAll(Arrays.asList(args).subList(i + 1, args.length));
                break;
            }
        }

        if (mainClass == null) throw new PreloaderException("no main class name provided");

        return new Options(classpath, mainClass, arguments);
    }

    private static List<File> parseClassPath(String classpath) {
        String[] paths = classpath.split(File.pathSeparator);
        List<File> files = new ArrayList<>(paths.length);
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                throw new PreloaderException("file does not exist: " + file);
            }
            files.add(file);
        }
        return files;
    }

    private static void printUsage(PrintStream out) {
        out.println("usage: java -jar kotlin-preloader.jar [<preloader-options>] <main-class> [<main-class-arguments>]");
        out.println("where possible options include:");
        out.println("  -classpath (-cp) <paths>    Paths where to find class files");
        out.println("  -help (-h)                  Output this help message");
    }

    private static class Options {
        public final List<File> classpath;
        public final String mainClass;
        public final List<String> arguments;

        private Options(List<File> classpath, String mainClass, List<String> arguments) {
            this.classpath = classpath;
            this.mainClass = mainClass;
            this.arguments = arguments;
        }
    }

    public static class PreloaderException extends RuntimeException {
        public PreloaderException(String message) {
            super(message);
        }
    }
}
