/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight.unwrap;

import com.intellij.codeInsight.unwrap.Unwrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.util.List;

public abstract class AbstractUnwrapRemoveTest extends LightCodeInsightTestCase {
    public void doTestExpressionRemover(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinExpressionRemover.class);
    }

    public void doTestThenUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinThenUnwrapper.class);
    }

    public void doTestElseUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinElseUnwrapper.class);
    }

    public void doTestElseRemover(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinElseRemover.class);
    }

    public void doTestLoopUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinLoopUnwrapper.class);
    }

    public void doTestTryUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinTryUnwrapper.class);
    }

    public void doTestCatchUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinCatchUnwrapper.class);
    }

    public void doTestCatchRemover(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinCatchRemover.class);
    }

    public void doTestFinallyUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinFinallyUnwrapper.class);
    }

    public void doTestFinallyRemover(@NotNull String path) throws Exception {
        doTest(path, KotlinUnwrappers.KotlinFinallyRemover.class);
    }

    public void doTestLambdaUnwrapper(@NotNull String path) throws Exception {
        doTest(path, KotlinLambdaUnwrapper.class);
    }

    private void doTest(@NotNull String path, final Class<? extends Unwrapper> unwrapperClass) throws Exception {
        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);

        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        String option = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// OPTION: ");
        Integer optionIndex = option != null ? Integer.parseInt(option) : 0;

        List<Pair<PsiElement, Unwrapper>> unwrappersWithPsi =
                new KotlinUnwrapDescriptor().collectUnwrappers(getProject(), getEditor(), getFile());

        if (isApplicableExpected) {
            final Pair<PsiElement, Unwrapper> selectedUnwrapperWithPsi = unwrappersWithPsi.get(optionIndex);
            assertEquals(unwrapperClass, selectedUnwrapperWithPsi.second.getClass());

            final PsiElement first = selectedUnwrapperWithPsi.first;

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    selectedUnwrapperWithPsi.second.unwrap(getEditor(), first);
                }
            });

            checkResultByFile(path + ".after");
        } else {
            assertTrue(
                    ContainerUtil.and(unwrappersWithPsi, new Condition<Pair<PsiElement, Unwrapper>>() {
                        @Override
                        public boolean value(Pair<PsiElement, Unwrapper> pair) {
                            return pair.second.getClass() != unwrapperClass;
                        }
                    })
            );
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
