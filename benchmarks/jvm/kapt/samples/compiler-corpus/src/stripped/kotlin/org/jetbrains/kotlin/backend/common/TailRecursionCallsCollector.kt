/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.usesDefaultArguments
import org.jetbrains.kotlin.ir.visitors.IrVisitor

data class TailCalls(val ir: Set<IrCall>, val fromManyFunctions: Boolean)

/**
 * Collects calls to be treated as tail recursion.
 * The checks are partially based on the frontend implementation
 * in `ControlFlowInformationProvider.markAndCheckRecursiveTailCalls()`.
 *
 * This analysis is not very precise and can miss some calls.
 * It is also not guaranteed that each returned call is detected as tail recursion by the frontend.
 * However any returned call can be correctly optimized as tail recursion.
 */
fun collectTailRecursionCalls(
    irFunction: IrFunction,
    followFunctionReference: (IrFunctionReference) -> Boolean,
    followRichFunctionReference: (IrRichFunctionReference) -> Boolean
): TailCalls { TODO() }
