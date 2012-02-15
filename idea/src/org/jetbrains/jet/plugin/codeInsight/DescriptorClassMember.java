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

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import javax.swing.*;

/**
 * @author yole
 */
public class DescriptorClassMember implements ClassMember {
    private final DeclarationDescriptor myDescriptor;

    public DescriptorClassMember(DeclarationDescriptor descriptor) {
        myDescriptor = descriptor;
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        final DeclarationDescriptor parent = myDescriptor.getContainingDeclaration();
        return new DescriptorClassMember(parent);
    }

    @Override
    public void renderTreeNode(SimpleColoredComponent component, JTree tree) {
        component.append(getText());
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

        if (myDescriptor != null ? !myDescriptor.equals(that.myDescriptor) : that.myDescriptor != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myDescriptor != null ? myDescriptor.hashCode() : 0;
    }
}
