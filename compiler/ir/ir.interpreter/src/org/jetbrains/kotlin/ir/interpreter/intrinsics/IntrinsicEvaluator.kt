/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.intrinsics

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.fqName

internal object IntrinsicEvaluator {
    private val fqNameToHandler: Map<String, IntrinsicBase> = buildMap {
        listOf(
            EmptyArray, ArrayOf, ArrayOfNulls, ArrayConstructor, EnumValues, EnumValueOf,
            JsPrimitives, SourceLocation, AssertIntrinsic, DataClassArrayToString, Indent
        ).forEach { intrinsic -> intrinsic.getListOfAcceptableFunctions().forEach { put(it, intrinsic) } }
    }

    fun unwindInstructions(irFunction: IrFunction, environment: IrInterpreterEnvironment): List<Instruction>? {
        val fqName = irFunction.fqName
        return fqNameToHandler[fqName]?.unwind(irFunction, environment) ?: when {
            EnumIntrinsics.canHandleFunctionWithName(fqName, irFunction.origin) -> EnumIntrinsics.unwind(irFunction, environment)
            else -> null
        }
    }
}