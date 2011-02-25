/*
 * @author max
 */
package org.jetbrains.jet.parsing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitor;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
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

    public static String getTestDataDir() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
        File root = new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class"));
        return FileUtil.toSystemIndependentName(root.getParentFile().getParentFile().getParent());
    }

    @Override
    protected void checkResult(@NonNls String targetDataName, PsiFile file) throws IOException {
        file.acceptChildren(new JetVisitor() {
            @Override
            public void visitJetElement(JetElement elem) {
                elem.acceptChildren(this);
                try {
                    checkPsiGetters(elem);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });

        super.checkResult(targetDataName, file);
    }

    private void checkPsiGetters(JetElement elem) throws Throwable {
        Method[] methods = elem.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("get") && !method.getName().startsWith("find")) continue;
            if (method.getParameterTypes().length > 0) continue;
            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.jet")) continue;

            Object result = method.invoke(elem);
            if (result == null) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation instanceof JetElement.IfNotParsed) {
                        assertNotNull(
                                "Imcomplete operation in parsed OK test, method " + method.getName() +
                                " in " + declaringClass.getSimpleName() + " returns null. Element text: \n" + elem.getText(),
                                PsiTreeUtil.findChildOfType(elem, PsiErrorElement.class));
                    }
                }
            }
        }
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
