/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

public class JetClsNavigationPolicy implements ClsCustomNavigationPolicy {
    @Override
    @Nullable
    public PsiElement getNavigationElement(@NotNull ClsClassImpl clsClass) {
        return getNavigationElementForMember(clsClass);
    }

    @Override
    @Nullable
    public PsiElement getNavigationElement(@NotNull ClsMethodImpl clsMethod) {
        return getNavigationElementForMember(clsMethod);
    }

    @Override
    @Nullable
    public PsiElement getNavigationElement(@NotNull ClsFieldImpl clsField) {
        return getNavigationElementForMember(clsField);
    }

    @Nullable
    private static PsiElement getNavigationElementForMember(@NotNull ClsMemberImpl clsMember) {
        VirtualFile virtualFile = clsMember.getContainingFile().getVirtualFile();
        if (virtualFile == null || !JetDecompiledData.isKotlinFile(clsMember.getProject(), virtualFile)) {
            return null;
        }

        JetDecompiledData decompiledData = JetDecompiledData.getDecompiledData((ClsFileImpl) clsMember.getContainingFile());
        JetDeclaration decompiledDeclaration = decompiledData.getJetDeclarationByClsElement(clsMember);

        if (decompiledDeclaration == null) {
            return null;
        }

        JetDeclaration sourceElement = decompiledDeclaration.accept(new SourceForDecompiledExtractingVisitor(), null);
        return sourceElement != null ? sourceElement : decompiledDeclaration;
    }

    private static class SourceForDecompiledExtractingVisitor extends JetVisitor<JetDeclaration, Void> {
        @Override
        public JetDeclaration visitNamedFunction(JetNamedFunction function, Void data) {
            return JetSourceNavigationHelper.getSourceFunction(function);
        }

        @Override
        public JetDeclaration visitProperty(JetProperty property, Void data) {
            return JetSourceNavigationHelper.getSourceProperty(property);
        }

        @Override
        public JetDeclaration visitObjectDeclaration(JetObjectDeclaration declaration, Void data) {
            return JetSourceNavigationHelper.getSourceClassOrObject(declaration);
        }

        @Override
        public JetDeclaration visitClass(JetClass klass, Void data) {
            return JetSourceNavigationHelper.getSourceClassOrObject(klass);
        }
    }
}
