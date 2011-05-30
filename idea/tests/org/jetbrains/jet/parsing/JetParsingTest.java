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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBase;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitor;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

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
            public void visitJetElement(JetElement element) {
                element.acceptChildren(this);
                try {
                    checkPsiGetters(element);
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
        JetTestCaseBase.NamedTestFactory factory = new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetParsingTest(dataPath, name);
            }
        };
        String prefix = JetParsingTest.getTestDataDir() + "/psi/";
        suite.addTest(JetTestCaseBase.suiteForDirectory(prefix, "/", false, factory));
        suite.addTest(JetTestCaseBase.suiteForDirectory(prefix, "examples", true, factory));
        return suite;
    }

}
