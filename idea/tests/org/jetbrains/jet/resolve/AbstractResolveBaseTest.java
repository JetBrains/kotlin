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

package org.jetbrains.jet.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.testFramework.LightCodeInsightTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ReferenceUtils;

import java.util.List;

public abstract class AbstractResolveBaseTest extends LightCodeInsightTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    protected void doTest(String path) {
        configureByFile(path);

        if (InTextDirectivesUtils.isDirectiveDefined(getFile().getText(), "MULTIRESOLVE")) {
            doMultiResolveTest();
        }
        else {
            doSingleResolveTest();
        }
    }

    protected static void doSingleResolveTest() {
        String referenceToString = InTextDirectivesUtils.findStringWithPrefixes(getFile().getText(), "REF:");
        Assert.assertNotNull("Test data wasn't found, use \"// REF: \" directive", referenceToString);

        int offset = getEditor().getCaretModel().getOffset();
        PsiReference psiReference = getFile().findReferenceAt(offset);
        if (psiReference != null) {
            PsiElement resolvedTo = psiReference.resolve();
            if (resolvedTo != null) {
                String resolvedToElementStr = ReferenceUtils.renderAsGotoImplementation(resolvedTo);
                String notEqualMessage = String.format("Found reference to '%s', but '%s' was expected",
                                                       resolvedToElementStr, referenceToString);
                assertEquals(notEqualMessage, referenceToString, resolvedToElementStr);
            }
            else {
                Assert.assertNull(
                        String.format("Element %s wasn't resolved to anything, but %s was expected", psiReference, referenceToString),
                        referenceToString);
            }
        }
        else {
            Assert.assertNull(String.format("No reference found at offset: %s, but one resolved to %s was expected", offset, referenceToString),
                              referenceToString);
        }
    }

    protected static void doMultiResolveTest() {
        List<String> expectedReferences = InTextDirectivesUtils.findListWithPrefixes(getFile().getText(), "REF:");

        PsiReference psiReference =
                getFile().findReferenceAt(getEditor().getCaretModel().getOffset());

        assertTrue(psiReference instanceof PsiPolyVariantReference);

        PsiPolyVariantReference variantReference = (PsiPolyVariantReference) psiReference;

        ResolveResult[] results = variantReference.multiResolve(true);

        List<String> actualResolvedTo = Lists.newArrayList();
        for (ResolveResult result : results) {
            PsiElement resolvedToElement = result.getElement();
            assertNotNull(resolvedToElement);

            actualResolvedTo.add(ReferenceUtils.renderAsGotoImplementation(resolvedToElement));
        }

        assertOrderedEquals(Ordering.natural().sortedCopy(actualResolvedTo), Ordering.natural().sortedCopy(expectedReferences));
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "./";
    }
}
