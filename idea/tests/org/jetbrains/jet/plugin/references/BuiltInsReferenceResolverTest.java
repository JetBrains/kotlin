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

package org.jetbrains.jet.plugin.references;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ResolveTestCase;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BuiltInsReferenceResolverTest extends ResolveTestCase {
    public void testAny() throws Exception {
        doTest();
    }

    public void testFunction() throws Exception {
        doTest();
    }

    public void testInt() throws Exception {
        doTest();
    }

    public void testNothing() throws Exception {
        doTest();
    }

    public void testEquals() throws Exception {
        doTest();
    }

    public void testToString() throws Exception {
        doTest();
    }

    public void testTimes() throws Exception {
        doTest();
    }

    public void testUnit() throws Exception {
        doTest();
    }

    public void testEmptyRange() throws Exception {
        doTest();
    }

    public void testIntArrayConstructor() throws Exception {
        doTest();
    }

    public void testAllReferencesResolved() {
        BuiltInsReferenceResolver referenceResolver = getProject().getComponent(BuiltInsReferenceResolver.class);
        for (DeclarationDescriptor descriptor : getAllStandardDescriptors()) {
            assertNotNull("Can't resolve " + descriptor, referenceResolver.resolveBuiltInSymbol(descriptor));
        }
    }

    private static Collection<DeclarationDescriptor> getAllStandardDescriptors() {
        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();

        PackageFragmentDescriptor builtinsPackageFragment = KotlinBuiltIns.getInstance().getBuiltInsPackageFragment();

        for (DeclarationDescriptor packageMember : builtinsPackageFragment.getMemberScope().getAllDescriptors()) {
            packageMember.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {
                @Override
                public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                    descriptors.add(descriptor);
                    for (DeclarationDescriptor classMember : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                        classMember.acceptVoid(this);
                    }
                    return null;
                }

                @Override
                public Void visitDeclarationDescriptor(DeclarationDescriptor descriptor, Void data) {
                    descriptors.add(descriptor);
                    return null;
                }
            });
        }

        return descriptors;
    }

    private void doTest() throws Exception {
        JetPsiReference reference = (JetPsiReference) configureByFile(getTestName(true) + ".kt");
        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertEquals(1, reference.multiResolve(false).length);

        List<PsiComment> comments = PsiTreeUtil.getChildrenOfTypeAsList(getFile(), PsiComment.class);
        String[] expectedTarget = comments.get(comments.size() - 1).getText().substring(2).split(":");
        assertEquals(2, expectedTarget.length);
        String expectedFile = expectedTarget[0];
        String expectedName = expectedTarget[1];

        PsiFile targetFile = resolved.getContainingFile();
        PsiDirectory targetDir = targetFile.getParent();
        assertNotNull(targetDir);
        assertEquals(expectedFile, targetDir.getName() + "/" + targetFile.getName());
        assertInstanceOf(resolved, PsiNamedElement.class);
        assertEquals(expectedName, ((PsiNamedElement) resolved).getName());
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/resolve/builtins/";
    }
}
