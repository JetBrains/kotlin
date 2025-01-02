/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

/**
 * Original element before inlining. Useful only with IR
 * inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
 * idempotence invariant and can contain a chain of declarations.
 *
 * `null` <=> `this` element wasn't inlined.
 */
var IrElement.originalBeforeInline: IrElement? by irAttribute(copyByDefault = true)