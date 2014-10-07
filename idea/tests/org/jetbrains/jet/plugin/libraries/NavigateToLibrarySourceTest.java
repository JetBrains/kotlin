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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor;
import org.jetbrains.jet.plugin.navigation.NavigationTestUtils;
import org.jetbrains.jet.plugin.references.JetReference;

import java.util.*;

/**
 * Attaching library with sources, and trying to navigate to its entities from source code.
 */
public class NavigateToLibrarySourceTest extends AbstractNavigateToLibraryTest {
    public void testEnum() {
        doTest();
    }

    public void testProperty() {
        doTest();
    }

    public void testGlobalProperty() {
        doTest();
    }

    public void testExtensionProperty() {
        doTest();
    }

    public void testGlobalFunction() {
        doTest();
    }

    public void testClassObject() {
        doTest();
    }

    public void testExtensionFunction() {
        doTest();
    }

    public void testSameNameInDifferentSources() {
        doTest();
    }

    public void testConstructor() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    public void testTypeWithSameShortName() {
        doTest();
    }

    public void testOverloadedFunWithTypeParam() {
        doTest();
    }

    public void testGenericFunctionWithInferredTypeArguments() {
        doTest();
    }

    public void testGenericFunctionWithExplicitlyDeclaredTypeArguments() {
        doTest();
    }

    private void doTest() {
        myFixture.configureByFile(TEST_DATA_PATH + "/usercode/" + getTestName(false) + ".kt");

        checkAnnotatedLibraryCode(false);
        checkAnnotatedLibraryCode(true);
    }

    @Override
    protected void tearDown() throws Exception {
        JetSourceNavigationHelper.setForceResolve(false);
        super.tearDown();
    }

    private void checkAnnotatedLibraryCode(boolean forceResolve) {
        JetSourceNavigationHelper.setForceResolve(forceResolve);
        String actualCode = NavigationTestUtils
                .getNavigateElementsText(myFixture.getProject(), collectInterestingNavigationElements());
        String expectedCode = getExpectedAnnotatedLibraryCode();
        assertSameLines(expectedCode, actualCode);
    }

    private Collection<JetReference> collectInterestingReferences() {
        PsiFile psiFile = myFixture.getFile();
        Map<PsiElement, JetReference> referenceContainersToReferences = new LinkedHashMap<PsiElement, JetReference>();
        for (int offset = 0; offset < psiFile.getTextLength(); offset++) {
            PsiReference ref = psiFile.findReferenceAt(offset);
            if (ref instanceof JetReference && !referenceContainersToReferences.containsKey(ref.getElement())) {
                PsiElement target = ref.resolve();
                if (target == null) continue;
                PsiFile targetNavPsiFile = target.getNavigationElement().getContainingFile();
                if (targetNavPsiFile == null) continue;
                VirtualFile targetNavFile = targetNavPsiFile.getVirtualFile();
                if (targetNavFile == null) continue;
                if (ProjectFileIndex.SERVICE.getInstance(getProject()).isInLibrarySource(targetNavFile)) {
                    referenceContainersToReferences.put(ref.getElement(), (JetReference)ref);
                }
            }
        }
        return referenceContainersToReferences.values();
    }

    private Collection<PsiElement> collectInterestingNavigationElements() {
        return ContainerUtil.map(collectInterestingReferences(), new Function<JetReference, PsiElement>() {
            @Override
            public PsiElement fun(JetReference reference) {
                PsiElement target = reference.resolve();
                assertNotNull(target);
                return target.getNavigationElement();
            }
        });
    }

    private String getExpectedAnnotatedLibraryCode() {
        Document document = myFixture.getDocument(myFixture.getFile());
        assertNotNull(document);
        return JetTestUtils.getLastCommentedLines(document);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/library", true);
    }
}
