/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import kotlin.SubclassOptInRequired;

/**
 * Represents a variable declaration introduced by {@code val} or {@code var}: a property, a local variable, or an entry of a
 * destructuring declaration.
 *
 * <p>This is the common base type for the concrete node types {@link KtProperty} and {@link KtDestructuringDeclarationEntry}. A variable
 * may have a declared type and an initializer.
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtVariableDeclaration extends KtCallableDeclaration, KtDeclarationWithInitializer, KtValVarKeywordOwner {
    /**
     * Returns {@code true} if this variable is mutable (declared with {@code var}), or {@code false} if it is read-only (declared with
     * {@code val}).
     */
    boolean isVar();
}
