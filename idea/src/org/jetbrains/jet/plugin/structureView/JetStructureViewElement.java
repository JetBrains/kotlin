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

package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.PsiIconUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetDescriptorIconProvider;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetStructureViewElement implements StructureViewTreeElement, ColoredItemPresentation {
    private final NavigatablePsiElement element;

    private String elementText;
    private Icon icon;

    public JetStructureViewElement(@NotNull NavigatablePsiElement element) {
        this.element = element;
    }

    public JetStructureViewElement(@NotNull JetFile fileElement) {
        element = fileElement;
    }

    @NotNull
    public NavigatablePsiElement getElement() {
        return element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        element.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return element.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return element.canNavigateToSource();
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        List<JetDeclaration> childrenDeclarations = getChildrenDeclarations();
        return ArrayUtil.toObjectArray(ContainerUtil.map(childrenDeclarations, new Function<JetDeclaration, TreeElement>() {
            @Override
            public TreeElement fun(JetDeclaration declaration) {
                return new JetStructureViewElement(declaration);
            }
        }), TreeElement.class);
    }

    @Nullable
    @Override
    public TextAttributesKey getTextAttributesKey() {
        if (element instanceof JetModifierListOwner && JetPsiUtil.isDeprecated((JetModifierListOwner) element)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES;
        }
        return null;
    }

    @Nullable
    @Override
    public String getPresentableText() {
        if (elementText == null) {
            elementText = getElementText(element, getDescriptor());
        }

        return elementText;
    }

    @Nullable
    @Override
    public String getLocationString() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        if (icon == null) {
            icon = getElementIcon(element, getDescriptor());
        }

        return icon;
    }

    @Nullable
    private DeclarationDescriptor getDescriptor() {
        if (!(element.isValid() && element instanceof JetDeclaration)) {
            return null;
        }

        JetDeclaration declaration = (JetDeclaration) element;
        if (declaration instanceof JetClassInitializer) {
            return null;
        }

        return ResolvePackage.getLazyResolveSession(declaration).resolveToDescriptor(declaration);
    }

    private List<JetDeclaration> getChildrenDeclarations() {
        if (element instanceof JetFile) {
            JetFile jetFile = (JetFile) element;
            return jetFile.getDeclarations();
        }
        else if (element instanceof JetClass) {
            JetClass jetClass = (JetClass) element;
            List<JetDeclaration> declarations = new ArrayList<JetDeclaration>();
            for (JetParameter parameter : jetClass.getPrimaryConstructorParameters()) {
                if (parameter.hasValOrVarNode()) {
                    declarations.add(parameter);
                }
            }
            declarations.addAll(jetClass.getDeclarations());
            return declarations;
        }
        else if (element instanceof JetClassOrObject) {
            return ((JetClassOrObject) element).getDeclarations();
        }
        else if (element instanceof JetClassObject) {
            JetObjectDeclaration objectDeclaration = ((JetClassObject) element).getObjectDeclaration();
            return objectDeclaration.getDeclarations();
        }

        return Collections.emptyList();
    }

    private static Icon getElementIcon(@NotNull NavigatablePsiElement navigatablePsiElement, @Nullable DeclarationDescriptor descriptor) {
        if (descriptor != null) {
            return JetDescriptorIconProvider.getIcon(descriptor, navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY);
        }

        return PsiIconUtil.getProvidersIcon(navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY);
    }

    @Nullable
    private static String getElementText(@NotNull NavigatablePsiElement navigatablePsiElement, @Nullable DeclarationDescriptor descriptor) {
        if (descriptor != null) {
            return DescriptorRenderer.STARTS_FROM_NAME_WITH_SHORT_TYPES.render(descriptor);
        }

        String text = navigatablePsiElement.getName();
        if (!StringUtil.isEmpty(text)) {
            return text;
        }

        if (navigatablePsiElement instanceof JetClassInitializer) {
            return "<class initializer>";
        }

        return null;
    }
}
