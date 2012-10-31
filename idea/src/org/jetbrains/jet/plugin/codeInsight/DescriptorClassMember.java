/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.generation.ClassMemberWithElement;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.plugin.JetDescriptorIconProvider;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import javax.swing.*;

/**
 * @author yole
 */
public class DescriptorClassMember implements ClassMemberWithElement {

    public static final String NO_PARENT_FOR = "No parent for ";
    @NotNull
    private final DeclarationDescriptor myDescriptor;
    private final PsiElement myPsiElement;

    public DescriptorClassMember(PsiElement element, @NotNull DeclarationDescriptor descriptor) {
        myPsiElement = element;
        myDescriptor = descriptor;
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        final DeclarationDescriptor parent = myDescriptor.getContainingDeclaration();
        PsiElement declaration = null;
        if (myPsiElement instanceof JetDeclaration) {
            // kotlin
            declaration = PsiTreeUtil.getStubOrPsiParentOfType(myPsiElement, JetNamedDeclaration.class);
        }
        else if (myPsiElement != null) {
            // java or bytecode
            declaration = ((PsiMember) myPsiElement).getContainingClass();
        }
        assert parent != null : NO_PARENT_FOR + myDescriptor;
        return new DescriptorClassMember(declaration, parent);
    }

    @Override
    public void renderTreeNode(SimpleColoredComponent component, JTree tree) {
        component.append(getText());
        if (myPsiElement != null && myPsiElement.isValid()) {
            component.setIcon(myPsiElement.getIcon(0));
        }
        else {
            component.setIcon(JetDescriptorIconProvider.getBaseIcon(myDescriptor));
        }
    }

    @Override
    public String getText() {
        return DescriptorRenderer.TEXT.render(myDescriptor);
    }

    public DeclarationDescriptor getDescriptor() {
        return myDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DescriptorClassMember that = (DescriptorClassMember) o;

        if (!myDescriptor.equals(that.myDescriptor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myDescriptor.hashCode();
    }

    @Override
    public PsiElement getElement() {
        return myPsiElement;
    }
}
