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
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.ui.Queryable;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JetStructureViewElement implements StructureViewTreeElement, Queryable {
    private final NavigatablePsiElement element;
    private final boolean isInherited;

    private KotlinStructureElementPresentation presentation;

    public JetStructureViewElement(@NotNull NavigatablePsiElement element, @NotNull DeclarationDescriptor descriptor, boolean isInherited) {
        this.element = element;
        this.isInherited = isInherited;

        if (!(element instanceof JetElement)) {
            // Avoid storing descriptor in fields
            presentation = new KotlinStructureElementPresentation(isInherited(), element, descriptor);
        }
    }

    public JetStructureViewElement(@NotNull NavigatablePsiElement element, boolean isInherited) {
        this.element = element;
        this.isInherited = isInherited;
    }

    public JetStructureViewElement(@NotNull JetFile fileElement) {
        element = fileElement;
        isInherited = false;
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
        if (presentation == null) {
            presentation = new KotlinStructureElementPresentation(isInherited(), element, getDescriptor());
        }

        return presentation;
    }

    @NotNull
    @Override
    public TreeElement[] getChildren() {
        List<JetDeclaration> childrenDeclarations = getChildrenDeclarations();
        return ArrayUtil.toObjectArray(ContainerUtil.map(childrenDeclarations, new Function<JetDeclaration, TreeElement>() {
            @Override
            public TreeElement fun(JetDeclaration declaration) {
                return new JetStructureViewElement(declaration, false);
            }
        }), TreeElement.class);
    }

    @TestOnly
    @Override
    public void putInfo(@NotNull Map<String, String> info) {
        info.put("text", getPresentation().getPresentableText());
        info.put("location", getPresentation().getLocationString());
    }

    public boolean isInherited() {
        return isInherited;
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

    @NotNull
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
}
