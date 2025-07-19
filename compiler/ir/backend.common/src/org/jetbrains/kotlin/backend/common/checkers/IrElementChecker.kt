/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.IrValidationError
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.IrElement
import kotlin.reflect.KClass

interface IrChecker : IrValidationError.Cause

abstract class IrElementChecker<in E : IrElement>(
    elementClass: KClass<E>,
) : IrChecker {
    internal val elementClass: Class<in E> = elementClass.java

    abstract fun check(element: E, context: CheckerContext)
}