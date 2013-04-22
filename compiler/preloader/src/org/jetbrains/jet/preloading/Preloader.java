package org.jetbrains.jet.preloading;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Preloader {

    public static final int PRELOADER_ARG_COUNT = 4;

    public static void main(String[] args) throws Exception {
        if (args.length < PRELOADER_ARG_COUNT) {
            printUsageAndExit();
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println("File does not exist: " + file);
            printUsageAndExit();
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

        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(file, classNumber, parent);

        Class<?> mainClass = preloaded.loadClass(mainClassCanonicalName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        mainMethod.invoke(0, new Object[] {Arrays.copyOfRange(args, PRELOADER_ARG_COUNT, args.length)});

        if (printTime) {
            long dt = System.nanoTime() - startTime;
            System.out.format("Total time: %.3fs\n", dt / 1e9);
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
        System.out.println("Usage: Preloader <path to jar> <main class> <class number estimate> <parameters to pass to the main class> <time|notime>");
        System.exit(1);
    }
}
