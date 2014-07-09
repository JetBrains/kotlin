package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlParser;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class AndroidXmlTest extends TestCaseWithTmpdir {
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @NotNull
    private static String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase() + "/android";
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
    protected static String loadOrCreate(File file, String data) throws IOException {
        try {
            return new Scanner(file, "UTF-8" ).useDelimiter("\\A").next();
        } catch (IOException e) {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.close();
            fail("Empty expected data, creating from actual");
            return data;
        }
    }

    public void testConverterOneFile() throws Exception {
        ArrayList<File> paths = new ArrayList<File>();
        paths.add(new File(getTestDataPath() + "/layout.xml"));
        AndroidUIXmlParser parser = new AndroidUIXmlParser(null, paths);

        String actual = parser.parse();
        String expected = loadOrCreate(new File(getTestDataPath() + "/layout.kt"), actual);

        assertEquals(expected, actual);
    }
}
