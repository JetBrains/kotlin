/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.types.IrErrorType

/**
 * Various settings used during deserialization of IR modules and IR files.
 *
 * @property allowErrorNodes Whether serialization of [IrErrorType] and [IrErrorDeclaration] is permitted.
 *   TODO Consider removing in KT-69714.
 * @property allowAlreadyBoundSymbols Don't attempt to create a new declaration (IR entity) during deserialization
 *   if it turns out that the symbol is already bound. This is needed for specific JVM-related scenarios when it's
 *   necessary to deserialize IR for already existing declarations.
 * @property deserializeFunctionBodies Whether to deserialize bodies of all functions ([DeserializeFunctionBodies.ALL]),
 *   only inline functions and their local functions ([DeserializeFunctionBodies.ONLY_INLINE]), or don't deserialize
 *   function bodies at all ([DeserializeFunctionBodies.NONE]).
 */
class IrDeserializationSettings(
    val allowErrorNodes: Boolean = false,
    val allowAlreadyBoundSymbols: Boolean = false,
    val deserializeFunctionBodies: DeserializeFunctionBodies = DeserializeFunctionBodies.ALL,
) {
    enum class DeserializeFunctionBodies { ALL, ONLY_INLINE, NONE }
}
