/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

/**
 * Represents an anonymous initializer: a piece of code that runs during initialization without introducing a named declaration.
 *
 * This is the common base for the concrete node types [KtClassInitializer] (an `init` block in a class or object) and [KtScriptInitializer]
 * (a top-level statement in a script).
 */
interface KtAnonymousInitializer : KtDeclaration, KtStatementExpression {
    /**
     * The class, object, or script that this initializer belongs to.
     */
    val containingDeclaration: KtDeclaration

    /**
     * The code executed by this initializer (typically a [KtBlockExpression]), or `null` if it is absent in incomplete code.
     */
    val body: KtExpression?
}

