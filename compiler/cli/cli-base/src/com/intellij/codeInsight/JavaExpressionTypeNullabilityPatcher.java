/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.codeInsight;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// This file was hijacked from com.jetbrains.intellij.java:java-psi-impl
// As we don't have access to com.jetbrains.intellij.java:java-analysis-impl containing implementations of this extension point,
// we had to simplify it to return always true. The module is compiled against JDK 25, so we can't depend on it
// and implementations can't be easily hijacked.

/**
 * An extension point which allows to patch the type nullability of expressions.
 */
@SuppressWarnings("unused")
public interface JavaExpressionTypeNullabilityPatcher {
    ExtensionPointName<JavaExpressionTypeNullabilityPatcher> EP_NAME = ExtensionPointName.create("com.intellij.java.expressionTypeNullabilityPatcher");

    /**
     * Patches the type nullability of the given expression using the extra-linguistic knowledge
     * (e.g., call to a known library method). May patch not only the top-level nullability but
     * also nested nullability as well. Implement with care, as this is computed when determining
     * the expression type, so it's easy to get into infinite recursion if other non-trivial operations
     * are performed on the expression.
     *
     * @param expression expression
     * @param type computed type with inherent nullability
     * @return patched type, or null if no patching is supported for a given expression. In this case,
     * the next patcher will be tried, if available.
     */
    @Nullable PsiType tryPatchType(@NotNull PsiExpression expression, @NotNull PsiType type);

    /**
     * Patches the type nullability of the given expression using all registered patchers.
     *
     * @param expression expression whose type should be patched
     * @param type computed type with inherent nullability
     * @return the patched type, or the originally computed type if no patcher wants to patch this expression
     */
    static @NotNull PsiType patchTypeNullability(@NotNull PsiExpression expression, @NotNull PsiType type) {
        return type;
    }
}
