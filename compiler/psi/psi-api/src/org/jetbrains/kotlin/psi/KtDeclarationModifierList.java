/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
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
public class KtDeclarationModifierList extends KtModifierList {
    public KtDeclarationModifierList(@NotNull ASTNode node) {
        super(node);
    }

    public KtDeclarationModifierList(@NotNull KotlinModifierListStub stub) {
        super(stub, KtStubBasedElementTypes.MODIFIER_LIST);
    }
}
