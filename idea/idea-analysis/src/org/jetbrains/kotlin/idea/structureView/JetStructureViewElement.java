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

package org.jetbrains.kotlin.idea.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.*;

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

        if (!(element instanceof KtElement)) {
            // Avoid storing descriptor in fields
            presentation = new KotlinStructureElementPresentation(isInherited(), element, descriptor);
        }
    }

    public JetStructureViewElement(@NotNull NavigatablePsiElement element, boolean isInherited) {
        this.element = element;
        this.isInherited = isInherited;
    }

    public JetStructureViewElement(@NotNull KtFile fileElement) {
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
        List<KtDeclaration> childrenDeclarations = getChildrenDeclarations();
        return ArrayUtil.toObjectArray(ContainerUtil.map(childrenDeclarations, new Function<KtDeclaration, TreeElement>() {
            @Override
            public TreeElement fun(KtDeclaration declaration) {
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
        if (!(element.isValid() && element instanceof KtDeclaration)) {
            return null;
        }

        final KtDeclaration declaration = (KtDeclaration) element;
        if (declaration instanceof KtClassInitializer) {
            return null;
        }

        return ApplicationManager.getApplication().runReadAction(new Computable<DeclarationDescriptor>() {
            @Override
            public DeclarationDescriptor compute() {
                if (!DumbService.isDumb(element.getProject())) {
                    return ResolutionUtils.resolveToDescriptor(declaration);
                }

                return null;
            }
        });
    }

    @NotNull
    private List<KtDeclaration> getChildrenDeclarations() {
        if (element instanceof KtFile) {
            KtFile jetFile = (KtFile) element;
            return jetFile.getDeclarations();
        }
        else if (element instanceof KtClass) {
            KtClass ktClass = (KtClass) element;
            List<KtDeclaration> declarations = new ArrayList<KtDeclaration>();
            for (KtParameter parameter : ktClass.getPrimaryConstructorParameters()) {
                if (parameter.hasValOrVar()) {
                    declarations.add(parameter);
                }
            }
            declarations.addAll(ktClass.getDeclarations());
            return declarations;
        }
        else if (element instanceof KtClassOrObject) {
            return ((KtClassOrObject) element).getDeclarations();
        }

        return Collections.emptyList();
    }
}
