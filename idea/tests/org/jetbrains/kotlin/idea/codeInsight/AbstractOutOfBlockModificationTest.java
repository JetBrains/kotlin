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

package org.jetbrains.kotlin.idea.codeInsight;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.io.IOException;

public abstract class AbstractOutOfBlockModificationTest extends KotlinLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/outOfBlock");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/outOfBlock";
    }

    protected void doTest(String path) throws IOException {
        myFixture.configureByFile(path);

        boolean expectedOutOfBlock = getExpectedOutOfBlockResult();
        boolean isSkipCheckDefined = InTextDirectivesUtils.isDirectiveDefined(myFixture.getFile().getText(), "SKIP_ANALYZE_CHECK");

        // Special behaviour in 173, should be dropped after abandoning this version.
        // BUNCH: 181
        if ("InGlobalPropertyWithGetter".equals(getTestName(false))) {
            String apiVersion = ApplicationInfo.getInstance().getApiVersion();
            if (apiVersion != null && apiVersion.contains("-173.")) {
                expectedOutOfBlock = true;
                isSkipCheckDefined = true;
            }
        }

        assertTrue("It's allowed to skip check with analyze only for tests where out-of-block is expected",
                   !isSkipCheckDefined || expectedOutOfBlock);


        PsiModificationTrackerImpl tracker =
                (PsiModificationTrackerImpl) PsiManager.getInstance(myFixture.getProject()).getModificationTracker();

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull("Should be valid element", element);

        long oobBeforeType = tracker.getOutOfCodeBlockModificationCount();
        long modificationCountBeforeType = tracker.getModificationCount();

        myFixture.type(getStringToType());
        PsiDocumentManager.getInstance(myFixture.getProject()).commitDocument(myFixture.getDocument(myFixture.getFile()));

        long oobAfterCount = tracker.getOutOfCodeBlockModificationCount();
        long modificationCountAfterType = tracker.getModificationCount();

        assertTrue("Modification tracker should always be changed after type", modificationCountBeforeType != modificationCountAfterType);

        assertEquals("Result for out of block test is differs from expected on element in file:\n" + FileUtil.loadFile(new File(path)),
                     expectedOutOfBlock, oobBeforeType != oobAfterCount);

        if (!isSkipCheckDefined) {
            checkOOBWithDescriptorsResolve(expectedOutOfBlock);
        }
    }

    private void checkOOBWithDescriptorsResolve(boolean expectedOutOfBlock) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ((PsiModificationTrackerImpl) PsiManager.getInstance(myFixture.getProject()).getModificationTracker())
                        .incOutOfCodeBlockModificationCounter();
            }
        });

        PsiElement updateElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
        KtExpression ktExpression = PsiTreeUtil.getParentOfType(updateElement, KtExpression.class, false);
        KtDeclaration ktDeclaration = PsiTreeUtil.getParentOfType(updateElement, KtDeclaration.class, false);
        KtElement ktElement = ktExpression != null ? ktExpression : ktDeclaration;

        if (ktElement == null) return;

        ResolutionFacade facade = ResolutionUtils.getResolutionFacade(ktElement.getContainingKtFile());
        ResolveSession session = facade.getFrontendService(ResolveSession.class);
        session.forceResolveAll();

        BindingContext context = session.getBindingContext();

        if (ktExpression != null && ktExpression != ktDeclaration) {
            @SuppressWarnings("ConstantConditions")
            boolean expressionProcessed = context.get(
                    BindingContext.PROCESSED,
                    ktExpression instanceof KtFunctionLiteral ? (KtLambdaExpression) ktExpression.getParent() : ktExpression) == Boolean.TRUE;

            assertEquals("Expected out-of-block should result expression analyzed and vise versa", expectedOutOfBlock,
                         expressionProcessed);
        }
        else {
            boolean declarationProcessed = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration) != null;
            assertEquals("Expected out-of-block should result declaration analyzed and vise versa", expectedOutOfBlock,
                         declarationProcessed);
        }
    }

    private String getStringToType() {
        String text = myFixture.getDocument(myFixture.getFile()).getText();
        String typeDirectives = InTextDirectivesUtils.findStringWithPrefixes(text, "TYPE:");

        return typeDirectives != null ? StringUtil.unescapeStringCharacters(typeDirectives) : "a";
    }

    private boolean getExpectedOutOfBlockResult() {
        String text = myFixture.getDocument(myFixture.getFile()).getText();

        boolean expectedOutOfBlock = false;
        if (text.startsWith("// TRUE")) {
            expectedOutOfBlock = true;
        }
        else if (text.startsWith("// FALSE")) {
            expectedOutOfBlock = false;
        }
        else {
            fail("Expectation of code block result test should be configured with " +
                 "\"// TRUE\" or \"// FALSE\" directive in the beginning of the file");
        }
        return expectedOutOfBlock;
    }
}
