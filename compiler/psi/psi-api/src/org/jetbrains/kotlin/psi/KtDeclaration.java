/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.util.ArrayFactory;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;

/**
 * Represents a Kotlin declaration: a construct that introduces a named or structural entity, such as a class, function, property, type
 * alias, parameter, or type parameter.
 *
 * <p>This is the common base type for all declaration nodes in the Kotlin PSI. Because local declarations may appear wherever a statement
 * is allowed, a declaration is also a {@link KtExpression}. Every declaration owns a modifier list ({@link KtModifierListOwner}) and may
 * carry a preceding KDoc comment.
 *
 * @see KtNamedDeclaration a declaration that introduces a name
 * @see KtClassOrObject
 * @see KtFunction
 * @see KtProperty
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtDeclaration extends KtExpression, KtModifierListOwner {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    KtDeclaration[] EMPTY_ARRAY = new KtDeclaration[0];

    /** A factory for creating arrays of {@link KtDeclaration}, used by the PSI child-access machinery. */
    ArrayFactory<KtDeclaration> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtDeclaration[count];

    /** Returns the KDoc comment attached to this declaration, or {@code null} if it has none. */
    @Nullable
    KDoc getDocComment();
}
