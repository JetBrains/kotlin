/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

/**
 * Static helpers for computing fully qualified names of {@link KtNamedDeclaration}s from their PSI structure.
 *
 * <p>Prefer {@link KtNamedDeclaration#getFqName()} in client code; these helpers back that computation by walking the
 * enclosing declarations and package directive.
 */
public final class KtNamedDeclarationUtil {
    @Nullable
    //NOTE: use JetNamedDeclaration#getFqName instead
    /*package private*/ static FqName getFQName(@NotNull KtNamedDeclaration namedDeclaration) {
        Name name = namedDeclaration.getNameAsName();
        if (name == null) {
            return null;
        }

        FqName parentFqName = getParentFqName(namedDeclaration);

        if (parentFqName == null) {
            return null;
        }

        return parentFqName.child(name);
    }

    /**
     * Returns the fully qualified name of the element that lexically encloses the given {@code namedDeclaration} — its
     * package for a top-level declaration, or the containing class, object, or script otherwise — or {@code null} if it
     * cannot be determined.
     */
    @Nullable
    public static FqName getParentFqName(@NotNull KtNamedDeclaration namedDeclaration) {
        PsiElement parent = namedDeclaration.getParent();
        if (parent instanceof KtClassBody) {
            // One nesting to KtClassBody doesn't affect the qualified name
            parent = KtPsiUtilKt.getContainingClassOrObject((KtClassBody)parent);
        }

        if (parent instanceof KtFile) {
            return ((KtFile) parent).getPackageFqName();
        }
        else if (parent instanceof KtNamedFunction || parent instanceof KtClass) {
            return getFQName((KtNamedDeclaration) parent);
        }
        else if (namedDeclaration instanceof KtParameter) {
            KtClassOrObject constructorClass = KtPsiUtil.getClassIfParameterIsProperty((KtParameter) namedDeclaration);
            if (constructorClass != null) {
                return getFQName(constructorClass);
            }
        }
        else if (parent instanceof KtObjectDeclaration) {
             return getFQName((KtNamedDeclaration) parent);
        }
        else if (parent instanceof KtBlockExpression) {
            PsiElement parentOfParent = parent.getParent();
            if (parentOfParent instanceof KtScript) {
                return ((KtScript)parentOfParent).getFqName();

            }
        }

        return null;
    }

    private KtNamedDeclarationUtil() {
    }
}
