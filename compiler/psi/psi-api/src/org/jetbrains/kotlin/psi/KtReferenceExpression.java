/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.kotlin.resolution.KtResolvable;

/**
 * Represents an expression that references a declaration and can therefore be resolved to it, such as a name reference,
 * an array access, or a call expression.
 *
 * <p>Being a reference is a <em>syntactic</em> property; to obtain the actual declaration a reference resolves to, use
 * the {@link KtResolvable} facilities together with the Analysis API. Resolution may fail (for example, in unresolved or
 * erroneous code), so callers must handle the absence of a target.
 */
public interface KtReferenceExpression extends KtExpression, KtResolvable {
}
