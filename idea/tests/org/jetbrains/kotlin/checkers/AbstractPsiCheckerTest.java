/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.testFramework.fixtures.impl.JavaCodeInsightTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.highlighter.NameHighlighter;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseKt;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

import static org.jetbrains.kotlin.resolve.lazy.ResolveSession.areDescriptorsCreatedForDeclaration;

public abstract class AbstractPsiCheckerTest extends KotlinLightCodeInsightFixtureTestCase {
    static int i = 0;


    @Override
    protected void setUp() {
        i++;
        super.setUp();
    }

    public void doTest(@NotNull VirtualFile file) throws Exception {
        myFixture.configureFromExistingVirtualFile(file);
        checkHighlighting(true, false, false);
        checkResolveToDescriptor();
    }

    public void doTest(@NotNull String filePath) throws Exception {
        myFixture.configureByFile(fileName());
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
            myFixture.configureByFile(fileName());

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
        System.out.println(i);
        PsiFile file = getFile();
        boolean configured = KotlinLightCodeInsightFixtureTestCaseKt.configureCompilerOptions(file.getText(), getProject(), getModule());
        try {
            if (file instanceof KtFile && ((KtFile) file).isScript() && myFixture instanceof JavaCodeInsightTestFixtureImpl) {
                ((JavaCodeInsightTestFixtureImpl) myFixture).canChangeDocumentDuringHighlighting(true);
            }
            return myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
        }
        catch (FileComparisonFailure e) {
            throw new FileComparisonFailure(e.getMessage(), e.getExpected(), e.getActual(),
                                            new File(e.getFilePath()).getAbsolutePath());
        } finally {
            if (configured) {
                KotlinLightCodeInsightFixtureTestCaseKt.rollbackCompilerOptions(getProject(), getModule());
            }
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
