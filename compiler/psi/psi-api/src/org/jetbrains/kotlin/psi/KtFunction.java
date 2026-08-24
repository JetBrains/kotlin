/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import kotlin.SubclassOptInRequired;

/**
 * Represents a function: a named function, a function literal (lambda or anonymous function), or a constructor.
 *
 * <p>This is the common base type for the concrete node types {@link KtNamedFunction}, {@link KtFunctionLiteral}, and
 * {@link KtConstructor}. As a {@link KtCallableDeclaration} it has value parameters, type parameters, and a return type, and as a
 * {@link KtDeclarationWithBody} it may have a block or expression body.
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtFunction extends KtDeclarationWithBody, KtCallableDeclaration {
    /**
     * Returns {@code true} if this function is declared inside a code block (for example, in the body of another function) rather than as a
     * member or top-level function.
     */
    boolean isLocal();
}
