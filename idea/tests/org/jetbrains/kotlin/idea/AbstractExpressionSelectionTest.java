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

package org.jetbrains.kotlin.idea;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil2;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.util.Collections;

public abstract class AbstractExpressionSelectionTest extends LightCodeInsightTestCase {

    public void doTestExpressionSelection(@NotNull String path) throws Exception {
        configureByFile(path);
        final String expectedExpression = KotlinTestUtils.getLastCommentInFile((KtFile) getFile());

        try {
            KotlinRefactoringUtil2.selectElement(
                    getEditor(),
                    (KtFile) getFile(),
                    Collections.singletonList(CodeInsightUtils.ElementKind.EXPRESSION),
                    new KotlinRefactoringUtil2.SelectElementCallback() {
                        @Override
                        public void run(@Nullable PsiElement element) {
                            assertNotNull("Selected expression mustn't be null", element);
                            assertEquals(expectedExpression, element.getText());
                        }
                    }
            );
        }
        catch (KotlinRefactoringUtil2.IntroduceRefactoringException e) {
            assertEquals(expectedExpression, "");
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
