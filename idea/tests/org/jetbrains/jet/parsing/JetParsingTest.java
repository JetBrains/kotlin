/*
 * @author max
 */
package org.jetbrains.jet.parsing;

import com.intellij.openapi.application.PathManager;
import com.intellij.testFramework.ParsingTestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JetParsingTest extends ParsingTestCase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    private final String name;

    public JetParsingTest(String dataPath, String name) {
        super(dataPath, "jet");
        this.name = name;
    }

    @Override
    protected String getTestDataPath() {
        return getTestDataDir();
    }

    private static String getTestDataDir() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    @Override
    protected void runTest() throws Throwable {
        doTest(true);
    }

    @Override
    public String getName() {
        return "test" + name;
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(suiteForDirectory("/", false));
        suite.addTest(suiteForDirectory("examples", true));
        return suite;
    }

    private static TestSuite suiteForDirectory(final String dataPath, boolean recursive) {
        TestSuite suite = new TestSuite(dataPath);
        final String extension = ".jet";
        FilenameFilter extensionFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        };
        File dir = new File(getTestDataDir() + "/psi/" + dataPath);
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        if (recursive) {
            List<File> subdirs = Arrays.asList(dir.listFiles(dirFilter));
            Collections.sort(subdirs);
            for (File subdir : subdirs) {
                suite.addTest(suiteForDirectory(dataPath + "/" + subdir.getName(), recursive));
            }
        }
        List<File> files = Arrays.asList(dir.listFiles(extensionFilter));
        Collections.sort(files);
        for (File file : files) {
            String fileName = file.getName();
            suite.addTest(new JetParsingTest(dataPath, fileName.substring(0, fileName.length() - extension.length())));
        }
        return suite;
    }

}
