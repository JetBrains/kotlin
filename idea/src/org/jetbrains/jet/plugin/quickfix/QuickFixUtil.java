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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

import static org.jetbrains.jet.plugin.project.AnalyzeSingleFileUtil.getContextForSingleFile;

public class QuickFixUtil {
    private QuickFixUtil() {
    }

    public static boolean removePossiblyWhiteSpace(ASTDelegatePsiElement element, PsiElement possiblyWhiteSpace) {
        if (possiblyWhiteSpace instanceof PsiWhiteSpace) {
            element.deleteChildInternal(possiblyWhiteSpace.getNode());
            return true;
        }
        return false;
    }

    @Nullable
    public static <T extends PsiElement> T getParentElementOfType(Diagnostic diagnostic, Class<T> aClass) {
        return PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), aClass, false);
    }

    @Nullable
    public static JetType getDeclarationReturnType(JetNamedDeclaration declaration) {
        PsiFile file = declaration.getContainingFile();
        if (!(file instanceof JetFile)) return null;
        BindingContext bindingContext = getContextForSingleFile((JetFile)file);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        if (!(descriptor instanceof CallableDescriptor)) return null;
        JetType type = ((CallableDescriptor) descriptor).getReturnType();
        if (type instanceof DeferredType) {
            type = ((DeferredType) type).getActualType();
        }
        return type;
    }

    @Nullable
    public static JetType findLowerBoundOfOverriddenCallablesReturnTypes(BindingContext context, JetDeclaration callable) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, callable);
        if (!(descriptor instanceof CallableDescriptor)) {
            return null;
        }

        JetType matchingReturnType = null;
        for (CallableDescriptor overriddenDescriptor : ((CallableDescriptor) descriptor).getOverriddenDescriptors()) {
            JetType overriddenReturnType = overriddenDescriptor.getReturnType();
            if (overriddenReturnType == null) {
                return null;
            }
            if (matchingReturnType == null || JetTypeChecker.INSTANCE.isSubtypeOf(overriddenReturnType, matchingReturnType)) {
                matchingReturnType = overriddenReturnType;
            }
            else if (!JetTypeChecker.INSTANCE.isSubtypeOf(matchingReturnType, overriddenReturnType)) {
                return null;
            }
        }
        return matchingReturnType;
    }

    public static boolean canModifyElement(@NotNull PsiElement element) {
        return element.isWritable() && !BuiltInsReferenceResolver.isFromBuiltIns(element);
    }
}
