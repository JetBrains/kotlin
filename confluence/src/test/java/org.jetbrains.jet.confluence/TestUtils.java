package org.jetbrains.jet.confluence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Natalia.Ukhorskaya
 */

public class TestUtils {

    public static String readFile(File file) throws IOException {
        FileReader reader = new FileReader(file);
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = reader.read()) >= 0) {
            builder.append((char) c);
        }
        reader.close();
        return builder.toString();
    }

    public static void writeFile(File file, String str) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(str);
        writer.close();
    }

    public static String divideResultForLines(String result) {
       return result.replaceAll("<div class=\"line\">", "\n<div class=\"line\">");
    }
}
