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

package org.jetbrains.kotlin.checkers;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.highlighter.NameHighlighter;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

import static org.jetbrains.kotlin.resolve.lazy.ResolveSession.areDescriptorsCreatedForDeclaration;

public abstract class AbstractPsiCheckerTest extends KotlinLightCodeInsightFixtureTestCase {
    public void doTest(@NotNull VirtualFile file) throws Exception {
        myFixture.configureFromExistingVirtualFile(file);
        checkHighlighting(true, false, false);
        checkResolveToDescriptor();
    }

    public void doTest(@NotNull String filePath) throws Exception {
        myFixture.configureByFile(filePath);
        checkHighlighting(true, false, false);
        checkResolveToDescriptor();
    }

    public void doTest(@NotNull String... filePath) throws Exception {
        myFixture.configureByFiles(filePath);
        checkHighlighting(true, false, false);
        checkResolveToDescriptor();
    }

    public void doTestWithInfos(@NotNull String filePath) throws Exception {
        try {
            myFixture.configureByFile(filePath);

            //noinspection unchecked
            myFixture.enableInspections(SpellCheckingInspection.class);

            NameHighlighter.INSTANCE.setNamesHighlightingEnabled(false);
            checkHighlighting(true, true, false);
            checkResolveToDescriptor();
        }
        finally {
            NameHighlighter.INSTANCE.setNamesHighlightingEnabled(true);
        }
    }

    protected long checkHighlighting(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings) {
        try {
            return myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
        }
        catch (FileComparisonFailure e) {
            throw new FileComparisonFailure(e.getMessage(), e.getExpected(), e.getActual(), new File(e.getFilePath()).getAbsolutePath());
        }
    }

    void checkResolveToDescriptor() {
        KtFile file = (KtFile) myFixture.getFile();
        file.accept(new KtTreeVisitorVoid() {
            @Override
            public void visitDeclaration(@NotNull KtDeclaration dcl) {
                if (areDescriptorsCreatedForDeclaration(dcl)) {
                    ResolutionUtils.unsafeResolveToDescriptor(dcl, BodyResolveMode.FULL); // check for exceptions
                }
                dcl.acceptChildren(this, null);
            }
        });
    }

    @Override
    protected String getTestDataPath() {
        return KotlinTestUtils.getTestsRoot(this.getClass());
    }

}
