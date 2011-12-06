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
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;

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
        super(dataPath, "jet", new JetParserDefinition());
        this.name = name;
    }

    @Override
    protected String getTestDataPath() {
        return getTestDataDir() + "/psi";
    }

    public static String getTestDataDir() {
        return getHomeDirectory() + "/compiler/testData";
    }

    private static String getHomeDirectory() {
        File root = new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class"));
        return FileUtil.toSystemIndependentName(root.getParentFile().getParentFile().getParent());
    }

    @Override
    protected void checkResult(@NonNls String targetDataName, PsiFile file) throws IOException {
        file.acceptChildren(new JetVisitorVoid() {
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
            String methodName = method.getName();
            if (!methodName.startsWith("get") && !methodName.startsWith("find") || methodName.equals("getReference") || methodName.equals("getReferences")) continue;
            if (method.getParameterTypes().length > 0) continue;
            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.jet")) continue;

            Object result = method.invoke(elem);
            if (result == null) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation instanceof JetElement.IfNotParsed) {
                        assertNotNull(
                                "Incomplete operation in parsed OK test, method " + methodName +
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
        JetTestCaseBuilder.NamedTestFactory factory = new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetParsingTest(dataPath, name);
            }
        };
        String prefix = JetParsingTest.getTestDataDir() + "/psi/";
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "/", false, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "examples", true, factory));
        return suite;
    }

}
