/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.parsing;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PathUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.TestsCompilerError;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class AbstractParsingTest extends KtParsingTestCase {

    @Override
    protected String getTestDataPath() {
        return KtTestUtil.getHomeDirectory();
    }

    public AbstractParsingTest() {
        super(".", "kt", new KotlinParserDefinition());
    }

    private static void checkPsiGetters(KtElement elem) throws Throwable {
        Method[] methods = elem.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (!methodName.startsWith("get") && !methodName.startsWith("find") ||
                methodName.equals("getReference") ||
                methodName.equals("getReferences") ||
                methodName.equals("getUseScope") ||
                methodName.equals("getPresentation")) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.getParameterTypes().length > 0) continue;

            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.kotlin")) continue;

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

    protected void doParsingTest(@NotNull String filePath) {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, null);
    }

    protected void doParsingTest(@NotNull String filePath, Function1<String, String> contentFilter) {
        doBaseTest(filePath, KtNodeTypes.KT_FILE, contentFilter);
    }

    protected void doExpressionCodeFragmentParsingTest(@NotNull String filePath) {
        doBaseTest(filePath, KtNodeTypes.EXPRESSION_CODE_FRAGMENT, null);
    }

    protected void doBlockCodeFragmentParsingTest(@NotNull String filePath) {
        doBaseTest(filePath, KtNodeTypes.BLOCK_CODE_FRAGMENT, null);
    }

    private void doBaseTest(@NotNull String filePath, @NotNull IElementType fileType, Function1<String, String> contentFilter) {
        try {
            doBaseTestImpl(filePath, fileType, contentFilter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doBaseTestImpl(@NotNull String filePath, @NotNull IElementType fileType, Function1<String, String> contentFilter) throws Exception {
        String fileContent = loadFile(filePath);

        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath));

        try {
            myFile = createFile(filePath, fileType, contentFilter != null ? contentFilter.invoke(fileContent) : fileContent);
            myFile.acceptChildren(new KtVisitorVoid() {
                @Override
                public void visitKtElement(@NotNull KtElement element) {
                    element.acceptChildren(this);
                    try {
                        checkPsiGetters(element);
                    }
                    catch (Throwable throwable) {
                        throw new TestsCompilerError(throwable);
                    }
                }
            });
        } catch (Throwable throwable) {
            throw new TestsCompilerError(throwable);
        }

        doCheckResult(myFullDataPath, filePath.replaceAll("\\.kts?", ".txt"), toParseTreeText(myFile, false, false).trim());
    }

    private PsiFile createFile(@NotNull String filePath, @NotNull IElementType fileType, @NotNull String fileContent) {
        KtPsiFactory psiFactory = new KtPsiFactory(myProject);

        if (fileType == KtNodeTypes.EXPRESSION_CODE_FRAGMENT) {
            return psiFactory.createExpressionCodeFragment(fileContent, null);
        }
        else if (fileType == KtNodeTypes.BLOCK_CODE_FRAGMENT) {
            return psiFactory.createBlockCodeFragment(fileContent, null);
        }
        else {
            return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), fileContent);
        }
    }
}
