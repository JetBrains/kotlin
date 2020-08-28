/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfo
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoFromCallHandler
import org.jetbrains.kotlin.backend.common.lower.loops.NestedHeaderInfoBuilderForWithIndex
import org.jetbrains.kotlin.backend.common.lower.loops.WithIndexHeaderInfo
import org.jetbrains.kotlin.backend.common.lower.matchers.Quantifier
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isIterable
import org.jetbrains.kotlin.ir.types.isSequence
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for calls to `withIndex()`. */
internal class WithIndexHandler(context: CommonBackendContext, private val visitor: NestedHeaderInfoBuilderForWithIndex) :
    HeaderInfoFromCallHandler<Nothing?> {

    // Use Quantifier.ANY so we can handle all `withIndex()` calls in the same manner.
    override val matcher =
        createIrCallMatcher(Quantifier.ANY) {
            callee {
                fqName { it == FqName("kotlin.collections.withIndex") }
                extensionReceiver { it != null && it.type.run { isArray() || isPrimitiveArray() || isIterable() } }
                parameterCount { it == 0 }
            }
            callee {
                fqName { it == FqName("kotlin.text.withIndex") }
                extensionReceiver { it != null && it.type.isSubtypeOfClass(context.ir.symbols.charSequence) }
                parameterCount { it == 0 }
            }
            callee {
                fqName { it == FqName("kotlin.sequences.withIndex") }
                extensionReceiver { it != null && it.type.run { isSequence() } }
                parameterCount { it == 0 }
            }
        }

    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo? {
        // WithIndexHeaderInfo is a composite that contains the HeaderInfo for the underlying iterable (if any).
        val nestedInfo = expression.extensionReceiver!!.accept(visitor, null) ?: return null

        // We cannot lower `iterable.withIndex().withIndex()`.
        // NestedHeaderInfoBuilderForWithIndex should not be yielding a WithIndexHeaderInfo, hence the assert.
        assert(nestedInfo !is WithIndexHeaderInfo)

        return WithIndexHeaderInfo(nestedInfo)
    }
}