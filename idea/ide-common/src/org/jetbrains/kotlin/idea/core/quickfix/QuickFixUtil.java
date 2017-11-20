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

package org.jetbrains.kotlin.idea.core.quickfix;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

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
    public static KotlinType getDeclarationReturnType(KtNamedDeclaration declaration) {
        PsiFile file = declaration.getContainingFile();
        if (!(file instanceof KtFile)) return null;
        DeclarationDescriptor descriptor = ResolutionUtils.unsafeResolveToDescriptor(declaration, BodyResolveMode.FULL);
        if (!(descriptor instanceof CallableDescriptor)) return null;
        KotlinType type = ((CallableDescriptor) descriptor).getReturnType();
        if (type instanceof DeferredType) {
            type = ((DeferredType) type).getDelegate();
        }
        return type;
    }

    @Nullable
    public static KotlinType findLowerBoundOfOverriddenCallablesReturnTypes(@NotNull CallableDescriptor descriptor) {
        KotlinType matchingReturnType = null;
        for (CallableDescriptor overriddenDescriptor : ((CallableDescriptor) descriptor).getOverriddenDescriptors()) {
            KotlinType overriddenReturnType = overriddenDescriptor.getReturnType();
            if (overriddenReturnType == null) {
                return null;
            }
            if (matchingReturnType == null || KotlinTypeChecker.DEFAULT.isSubtypeOf(overriddenReturnType, matchingReturnType)) {
                matchingReturnType = overriddenReturnType;
            }
            else if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(matchingReturnType, overriddenReturnType)) {
                return null;
            }
        }
        return matchingReturnType;
    }

    @Nullable
    public static PsiElement safeGetDeclaration(@Nullable CallableDescriptor descriptor) {
        //do not create fix if descriptor has more than one overridden declaration
        if (descriptor == null || descriptor.getOverriddenDescriptors().size() > 1) return null;
        return DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
    }

    @Nullable
    public static KtParameter getParameterDeclarationForValueArgument(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable ValueArgument valueArgument
    ) {
        PsiElement declaration = safeGetDeclaration(CallUtilKt.getParameterForArgument(resolvedCall, valueArgument));
        return declaration instanceof KtParameter ? (KtParameter) declaration : null;
    }

    private static boolean equalOrLastInThenOrElse(KtExpression thenOrElse, KtExpression expression) {
        if (thenOrElse == expression) return true;
        return thenOrElse instanceof KtBlockExpression && expression.getParent() == thenOrElse &&
               PsiTreeUtil.getNextSiblingOfType(expression, KtExpression.class) == null;
    }

    @Nullable
    public static KtIfExpression getParentIfForBranch(@Nullable KtExpression expression) {
        KtIfExpression ifExpression = PsiTreeUtil.getParentOfType(expression, KtIfExpression.class, true);
        if (ifExpression == null) return null;
        if (equalOrLastInThenOrElse(ifExpression.getThen(), expression)
            || equalOrLastInThenOrElse(ifExpression.getElse(), expression)) {
            return ifExpression;
        }
        return null;
    }

    // Returns true iff parent's value always or sometimes is evaluable to child's value, e.g.
    // parent = (x), child = x;
    // parent = if (...) x else y, child = x;
    // parent = y.x, child = x
    public static boolean canEvaluateTo(KtExpression parent, KtExpression child) {
        if (parent == null || child == null) {
            return false;
        }
        while (parent != child) {
            PsiElement childParent = child.getParent();
            if (childParent instanceof KtParenthesizedExpression) {
                child = (KtExpression) childParent;
                continue;
            }
            if (childParent instanceof KtDotQualifiedExpression &&
                (child instanceof KtCallExpression || child instanceof KtDotQualifiedExpression)) {
                child = (KtExpression) childParent;
                continue;
            }
            child = getParentIfForBranch(child);
            if (child == null) return false;
        }
        return true;
    }

    public static boolean canFunctionOrGetterReturnExpression(@NotNull KtDeclaration functionOrGetter, @NotNull KtExpression expression) {
        if (functionOrGetter instanceof KtFunctionLiteral) {
            KtBlockExpression functionLiteralBody = ((KtFunctionLiteral) functionOrGetter).getBodyExpression();
            PsiElement returnedElement = functionLiteralBody == null ? null : functionLiteralBody.getLastChild();
            return returnedElement instanceof KtExpression && canEvaluateTo((KtExpression) returnedElement, expression);
        }
        else {
            if (functionOrGetter instanceof KtDeclarationWithInitializer && canEvaluateTo(((KtDeclarationWithInitializer) functionOrGetter).getInitializer(), expression)) {
                return true;
            }
            KtReturnExpression returnExpression = PsiTreeUtil.getParentOfType(expression, KtReturnExpression.class);
            return returnExpression != null && canEvaluateTo(returnExpression.getReturnedExpression(), expression);
        }
    }

    public static String renderTypeWithFqNameOnClash(KotlinType type, String nameToCheckAgainst) {
        FqName fqNameToCheckAgainst = new FqName(nameToCheckAgainst);
        ClassifierDescriptor typeClassifierDescriptor = type.getConstructor().getDeclarationDescriptor();
        FqName typeFqName = typeClassifierDescriptor != null ? DescriptorUtils.getFqNameSafe(typeClassifierDescriptor) : fqNameToCheckAgainst;
        DescriptorRenderer renderer = typeFqName.shortName().equals(fqNameToCheckAgainst.shortName())
               ? IdeDescriptorRenderers.SOURCE_CODE
               : IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES;
        return renderer.renderType(type);
    }
}
