/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.parsing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.vcsUtil.VcsFileUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.IfNotParsed;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetIdeTemplate;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JetParsingTest extends ParsingTestCase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    private final String name;

    public JetParsingTest(@NotNull File file) {
        super(VcsFileUtil.relativePath(new File(getPsiTestDataDir()), file.getParentFile()), file.getName().replaceFirst(".*\\.", ""), new JetParserDefinition());
        this.name = file.getName().replaceFirst("\\.[^.]*$", "");
    }

    @Override
    protected String getTestDataPath() {
        return getPsiTestDataDir();
    }

    private static String getPsiTestDataDir() {
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
            if (!methodName.startsWith("get") && !methodName.startsWith("find") || methodName.equals("getReference") ||
                methodName.equals("getReferences") || methodName.equals("getUseScope")) continue;
            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.getParameterTypes().length > 0) continue;
            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.jet")) continue;

            Object result = method.invoke(elem);
            if (result == null) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation instanceof IfNotParsed) {
                        assertTrue(
                                "Incomplete operation in parsed OK test, method " + methodName +
                                " in " + declaringClass.getSimpleName() + " returns null. Element text: \n" + elem.getText(),
                                PsiTreeUtil.findChildOfType(elem, PsiErrorElement.class) == null
                                || PsiTreeUtil.findChildOfType(elem, JetIdeTemplate.class) == null);
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
                return new JetParsingTest(file);
            }
        };
        String prefix = JetParsingTest.getTestDataDir() + "/psi/";
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "/", false, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "examples", true, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "greatSyntacticShift", true, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "script", true, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "recovery", true, factory));
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(prefix, "propertyDelegate", true, factory));
        return suite;
    }

}
