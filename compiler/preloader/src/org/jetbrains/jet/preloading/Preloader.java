package org.jetbrains.jet.preloading;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Preloader {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
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

        ClassLoader parent = Preloader.class.getClassLoader();

        ClassLoader preloaded = ClassPreloadingUtils.preloadClasses(file, classNumber, parent);

        Class<?> mainClass = preloaded.loadClass(mainClassCanonicalName);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        mainMethod.invoke(0, new Object[] {Arrays.copyOfRange(args, 2, args.length)});
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: Preloader <path to jar> <main class> <class number estimate> <parameters to pass to the main class>");
        System.exit(1);
    }
}
