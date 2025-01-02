/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

/**
 * Original element before inlining. Useful only with IR
 * inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
 * idempotence invariant and can contain a chain of declarations.
 *
 * `null` <=> `this` element wasn't inlined.
 */
var IrElement.originalBeforeInline: IrElement? by irAttribute(followAttributeOwner = false)

/**
 * For annotation calls, shows if the original annotation has the all: use-site target.
 * It's necessary for implementation of RECORD_COMPONENT annotating, that should work only in case when we have this use-site target.
 *
 * @return true for an annotation with all: use-site target, null otherwise.
 */
var IrConstructorCall.isAnnotationWithAllUseSiteTarget: Boolean? by irAttribute(followAttributeOwner = false)
