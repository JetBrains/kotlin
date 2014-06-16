/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;
import org.jetbrains.jet.test.util.UtilPackage;
import org.jetbrains.jet.testing.ReferenceUtils;

import java.util.List;
import java.util.Set;

public abstract class AbstractReferenceResolveTest extends LightPlatformCodeInsightFixtureTestCase {
    public static class ExpectedResolveData {
        private final Boolean shouldBeUnresolved;
        private final String referenceToString;

        public ExpectedResolveData(Boolean shouldBeUnresolved, String referenceToString) {
            this.shouldBeUnresolved = shouldBeUnresolved;
            this.referenceToString = referenceToString;
        }

        public boolean shouldBeUnresolved() {
            return shouldBeUnresolved;
        }

        public String getReferenceString() {
            return referenceToString;
        }
    }

    public static final String MULTIRESOLVE = "MULTIRESOLVE";
    public static final String REF_EMPTY = "REF_EMPTY";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
        VfsRootAccess.allowRootAccess(JetTestCaseBuilder.getHomeDirectory());
    }

    @Override
    protected void tearDown() throws Exception {
        VfsRootAccess.disallowRootAccess(JetTestCaseBuilder.getHomeDirectory());

        Set<JetFile> builtInsSources = getProject().getComponent(BuiltInsReferenceResolver.class).getBuiltInsSources();
        FileManager fileManager = ((PsiManagerEx) PsiManager.getInstance(getProject())).getFileManager();

        super.tearDown();

        // Restore mapping between PsiFiles and VirtualFiles dropped in FileManager.cleanupForNextTest(),
        // otherwise built-ins psi elements will become invalid in next test.
        for (JetFile source : builtInsSources) {
            FileViewProvider provider = source.getViewProvider();
            fileManager.setViewProvider(provider.getVirtualFile(), provider);
        }
    }

    protected void doTest(String path) {
        assert path.endsWith(".kt") : path;
        UtilPackage.configureWithExtraFile(myFixture, path, ".Data");
        performChecks();
    }

    protected void performChecks() {
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.getFile().getText(), MULTIRESOLVE)) {
            doMultiResolveTest();
        }
        else {
            doSingleResolveTest();
        }
    }

    protected void doSingleResolveTest() {
        ExpectedResolveData expectedResolveData = readResolveData(myFixture.getFile().getText());

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiReference psiReference = myFixture.getFile().findReferenceAt(offset);

        checkReferenceResolve(expectedResolveData, offset, psiReference);
    }

    protected void doMultiResolveTest() {
        List<String> expectedReferences = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.getFile().getText(), "REF:");

        PsiReference psiReference = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());

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

    @NotNull
    public static ExpectedResolveData readResolveData(String fileText) {
        boolean shouldBeUnresolved = InTextDirectivesUtils.isDirectiveDefined(fileText, REF_EMPTY);
        List<String> refs = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "REF:");

        String referenceToString;
        if (shouldBeUnresolved) {
            Assert.assertTrue("REF: directives will be ignored for " + REF_EMPTY + " test: " + refs, refs.isEmpty());
            referenceToString = "<empty>";
        }
        else {
            assertTrue("Must be a single ref: " + refs + ".\n" +
                       "Use " + MULTIRESOLVE + " if you need multiple refs\n" +
                       "Use "+ REF_EMPTY + " for an unresolved reference",
                       refs.size() == 1);
            referenceToString = refs.get(0);
            Assert.assertNotNull("Test data wasn't found, use \"// REF: \" directive", referenceToString);
        }

        return new ExpectedResolveData(shouldBeUnresolved, referenceToString);
    }

    public static void checkReferenceResolve(ExpectedResolveData expectedResolveData, int offset, PsiReference psiReference) {
        if (psiReference != null) {
            PsiElement resolvedTo = psiReference.resolve();
            if (resolvedTo != null) {
                String resolvedToElementStr = ReferenceUtils.renderAsGotoImplementation(resolvedTo);
                String notEqualMessage = String.format("Found reference to '%s', but '%s' was expected",
                                                       resolvedToElementStr, expectedResolveData.getReferenceString());
                assertEquals(notEqualMessage, expectedResolveData.getReferenceString(), resolvedToElementStr);
            }
            else {
                if (!expectedResolveData.shouldBeUnresolved()) {
                    Assert.assertNull(
                            String.format("Element %s wasn't resolved to anything, but %s was expected",
                                          psiReference, expectedResolveData.getReferenceString()),
                            expectedResolveData.getReferenceString());
                }
            }
        }
        else {
            Assert.assertNull(
                    String.format("No reference found at offset: %s, but one resolved to %s was expected",
                                  offset, expectedResolveData.getReferenceString()),
                    expectedResolveData.getReferenceString());
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "./";
    }
}
