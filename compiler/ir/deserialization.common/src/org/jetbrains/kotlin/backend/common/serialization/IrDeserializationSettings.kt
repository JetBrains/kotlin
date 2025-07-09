/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrErrorType

/**
 * Various settings used during deserialization of IR modules and IR files.
 *
 * @property allowErrorNodes Whether serialization of [IrErrorType] is permitted.
 *   TODO Consider removing in KT-65375.
 * @property allowAlreadyBoundSymbols Don't attempt to create a new declaration (IR entity) during deserialization
 *   if it turns out that the symbol is already bound. This is needed for specific JVM-related scenarios when it's
 *   necessary to deserialize IR for already existing declarations.
 * @property deserializeFunctionBodies Whether to deserialize bodies of all functions ([DeserializeFunctionBodies.ALL]),
 *   only inline functions and their local functions ([DeserializeFunctionBodies.ONLY_INLINE]), or don't deserialize
 *   function bodies at all ([DeserializeFunctionBodies.NONE]).
 * @property useNullableAnyAsAnnotationConstructorCallType Whether to use `kotlin/Any?` as the type of the
 *   annotation [IrConstructorCall] instead of the lazy type that is computed by [IrConstructorCall.symbol].
 *   This setting is necessary for deserialization of unbound IR, where [IrConstructorCall.symbol] can happen
 *   to be unbound resulting in "X is unbound" crash on the first attempt to read annotation's type.
 *   See [org.jetbrains.kotlin.backend.common.serialization.IrBodyDeserializer.IrAnnotationType] for more details.
 */
class IrDeserializationSettings(
    val allowErrorNodes: Boolean = false,
    val allowAlreadyBoundSymbols: Boolean = false,
    val deserializeFunctionBodies: DeserializeFunctionBodies = DeserializeFunctionBodies.ALL,
    val useNullableAnyAsAnnotationConstructorCallType: Boolean = false,
) {
    enum class DeserializeFunctionBodies { ALL, ONLY_INLINE, NONE }
}
