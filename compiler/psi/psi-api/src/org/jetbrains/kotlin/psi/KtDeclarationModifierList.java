/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub;

/**
 * Represents a modifier list attached to a declaration.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    public abstract class Foo
 * // ^___________________^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtDeclarationModifierList extends KtModifierList {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtDeclarationModifierList[] EMPTY_ARRAY = new KtDeclarationModifierList[0];

    @KtImplementationDetail
    public KtDeclarationModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtDeclarationModifierList(@NotNull KotlinModifierListStub stub) {
        super(stub, KtNodeTypes.MODIFIER_LIST);
    }
}
