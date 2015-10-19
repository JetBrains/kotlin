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

package org.jetbrains.kotlin.resolve.lazy;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;

public class DeclarationScopeProviderImpl implements DeclarationScopeProvider {

    private final LazyDeclarationResolver lazyDeclarationResolver;

    private final FileScopeProvider fileScopeProvider;

    public DeclarationScopeProviderImpl(
            @NotNull LazyDeclarationResolver lazyDeclarationResolver,
            @NotNull FileScopeProvider fileScopeProvider
    ) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
        this.fileScopeProvider = fileScopeProvider;
    }

    @Override
    @NotNull
    public LexicalScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        JetDeclaration jetDeclaration = JetStubbedPsiUtil.getPsiOrStubParent(elementOfDeclaration, JetDeclaration.class, false);

        assert !(elementOfDeclaration instanceof JetDeclaration) || jetDeclaration == elementOfDeclaration :
                "For JetDeclaration element getParentOfType() should return itself.";
        assert jetDeclaration != null : "Should be contained inside declaration.";

        JetDeclaration parentDeclaration = JetStubbedPsiUtil.getContainingDeclaration(jetDeclaration);

        if (jetDeclaration instanceof JetPropertyAccessor) {
            parentDeclaration = JetStubbedPsiUtil.getContainingDeclaration(parentDeclaration, JetDeclaration.class);
        }

        if (parentDeclaration == null) {
            return fileScopeProvider.getFileScope((JetFile) elementOfDeclaration.getContainingFile());
        }

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) lazyDeclarationResolver.getClassDescriptor(classOrObject, NoLookupLocation.WHEN_GET_DECLARATION_SCOPE);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForInitializerResolution();
            }
            if (jetDeclaration instanceof JetObjectDeclaration
                || (jetDeclaration instanceof JetClass && !((JetClass) jetDeclaration).isInner())) {
                return classDescriptor.getScopeForStaticMemberDeclarationResolution();
            }

            return classDescriptor.getScopeForMemberDeclarationResolution();
        }

        throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + "\n" +
                                        PsiUtilsKt.getElementTextWithContext(jetDeclaration));
    }

    @NotNull
    @Override
    public DataFlowInfo getOuterDataFlowInfoForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        return DataFlowInfo.EMPTY;
    }
}
