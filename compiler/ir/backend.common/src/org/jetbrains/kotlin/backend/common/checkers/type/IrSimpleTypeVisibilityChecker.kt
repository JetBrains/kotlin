/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.type

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.checkVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType

internal object IrSimpleTypeVisibilityChecker: IrTypeChecker {
    override fun check(
        type: IrType,
        container: IrElement,
        context: CheckerContext,
    ) {
        if (type is IrSimpleType) {
            checkVisibility(type.classifier, container, context)
        }
    }
}