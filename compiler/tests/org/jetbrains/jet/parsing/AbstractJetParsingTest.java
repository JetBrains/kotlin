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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.IfNotParsed;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class AbstractJetParsingTest extends ParsingTestCase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getHomeDirectory();
    }

    public AbstractJetParsingTest() {
        super(".", "kt", new JetParserDefinition());
    }

    private static void checkPsiGetters(JetElement elem) throws Throwable {
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
                        assertNotNull(
                                "Incomplete operation in parsed OK test, method " + methodName +
                                " in " + declaringClass.getSimpleName() + " returns null. Element text: \n" + elem.getText(),
                                PsiTreeUtil.findChildOfType(elem, PsiErrorElement.class));
                    }
                }
            }
        }
    }

    protected void doParsingTest(@NotNull String filePath) throws Exception {
        myFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), loadFile(filePath));

        myFile.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement element) {
                element.acceptChildren(this);
                try {
                    checkPsiGetters(element);
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });

        doCheckResult(myFullDataPath, filePath.replace(".kt", ".txt"), toParseTreeText(myFile, false, false).trim());
    }
}
