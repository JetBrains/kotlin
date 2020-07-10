/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoFromCallHandler
import org.jetbrains.kotlin.backend.common.lower.matchers.Quantifier
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for calls to reverse an iterable. */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class ReversedHandler(context: CommonBackendContext, private val visitor: HeaderInfoBuilder) :
    HeaderInfoFromCallHandler<Nothing?> {

    private val symbols = context.ir.symbols

    // Use Quantifier.ANY so we can handle all reversed iterables in the same manner.
    override val matcher =
        createIrCallMatcher(Quantifier.ANY) {
            // Matcher for reversed progression.
            callee {
                fqName { it == FqName("kotlin.ranges.reversed") }
                extensionReceiver { it != null && it.type.toKotlinType() in symbols.progressionClassesTypes }
                parameterCount { it == 0 }
            }

            // TODO: Handle reversed String, Progression.withIndex(), etc.
        }

    // Reverse the HeaderInfo from the underlying progression or array (if any).
    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        expression.extensionReceiver!!.accept(visitor, null)?.asReversed()
}