/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Various settings used during deserialization of IR files.
 *
 * @property allowErrorNodes Whether serialization of [IrErrorType] and [IrErrorExpression] is permitted.
 *   Caution: This setting is not safe to use, as it can lead to crashes in the frontend or backend.
 *   The only legal case for using this setting is the `dump-ir` command of the `klib` command-line tool.
 * @property deserializeFunctionBodies Whether to deserialize bodies of all functions ([DeserializeFunctionBodies.ALL]),
 *   only inline functions and their local functions ([DeserializeFunctionBodies.ONLY_INLINE]), or don't deserialize
 *   function bodies at all ([DeserializeFunctionBodies.NONE]).
 * @property nullableAnyAsAnnotationConstructorCallType If set, then this type will be used as the type of the
 *   annotation [IrAnnotation] instead of the lazy type that is computed by [IrAnnotation.symbol].
 *   This setting is necessary for deserialization of unbound IR, where [IrAnnotation.symbol] can happen
 *   to be unbound resulting in "X is unbound" crash on the first attempt to read annotation's type.
 *   See [org.jetbrains.kotlin.backend.common.serialization.IrBodyDeserializer.IrAnnotationType] for more details.
 * @property fixSwappedKProperty2TypeParameterOrder If `true`, the type parameter order of `KProperty2`/`KMutableProperty2`
 *   in deserialized property references is swapped to compensate for the legacy order that was used in KLIBs
 *   compiled with Kotlin <= 2.1 (ABI version <= 1.201.0). See KT-75112, KT-86180.
 * @property deserializeTypeAliases Whether to deserialize [org.jetbrains.kotlin.ir.declarations.IrTypeAlias] declarations.
 *   Type aliases don't need to be (de)serialized anymore. See KT-86632.
 *   The only case for enabling it is the klib tool and its ability to dump (even older) KLIBs.
 * @property eagerlyDeserializeExplicitlyExportedDeclarations Whether the file should be immediately enqueued for
 *   deserialization if it has explicitly exported declarations (e.g. top-level initializers).
 *   Explicitly exported declarations are always scheduled to be deserialized before any other declarations of the file.
 *   If `false`, they are deserialized only when the file gets enqueued for another reason, for example,
 *   when some declaration in the file turns out to be reachable.
 * @property deserializeAllTopLevelDeclarations Whether to schedule deserialization of all top-level declarations
 *   in the file, no matter if they are reachable from other deserialized declarations or not.
 *   If `false`, only the declarations that are actually reachable are deserialized. */
class IrDeserializationSettings(
    val allowErrorNodes: Boolean = false,
    val deserializeFunctionBodies: DeserializeFunctionBodies = DeserializeFunctionBodies.ALL,
    val nullableAnyAsAnnotationConstructorCallType: IrType? = null,
    val fixSwappedKProperty2TypeParameterOrder: Boolean = false,
    val deserializeTypeAliases: Boolean = false,
    val eagerlyDeserializeExplicitlyExportedDeclarations: Boolean = false,
    val deserializeAllTopLevelDeclarations: Boolean = false,
) {
    enum class DeserializeFunctionBodies { ALL, ONLY_INLINE, NONE }
}
