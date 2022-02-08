/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.SpecialMethodWithDefaultInfo
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.org.objectweb.asm.commons.Method

// Represents a special bridge to `overridden`. Special bridges are overrides for Java methods which are
// exposed to Kotlin with a different name or different types. Typically, the Java version of a method is
// non-generic, while Kotlin exposes a generic method. In this case, the bridge method needs to perform
// additional type checking at runtime. The behavior in case of type errors is configured in `methodInfo`.
//
// Since special bridges may be exposed to Java as non-synthetic methods, we need correct generic signatures.
// There are a total of seven generic special bridge methods (Map.getOrDefault, Map.get, MutableMap.remove with
// only one argument, Map.keys, Map.values, Map.entries, and MutableList.removeAt). Of these seven there is only
// one which the JVM backend currently handles correctly (MutableList.removeAt). For the rest, it's impossible
// to reproduce the behavior of the JVM backend in this lowering.
//
// Finally, we sometimes need to use INVOKESPECIAL to invoke an existing special bridge implementation in a
// superclass, which is what `superQualifierSymbol` is for.
data class SpecialBridge(
    val overridden: IrSimpleFunction,
    val signature: Method,
    // We need to produce a generic signature if the underlying Java method contains type parameters.
    // E.g., the `java.util.Map<K, V>.keySet` method has a return type of `Set<K>`, and hence overrides
    // need to generate a generic signature.
    val needsGenericSignature: Boolean = false,
    // The result of substituting type parameters in the overridden Java method. This is different from
    // substituting into the overridden Kotlin method. For example, Map.getOrDefault has two arguments
    // with generic types in Kotlin, but only the second parameter is generic in Java.
    // May be null if the underlying Java method does not contain generic types.
    val substitutedParameterTypes: List<IrType>? = null,
    val substitutedReturnType: IrType? = null,
    val methodInfo: SpecialMethodWithDefaultInfo? = null,
    val superQualifierSymbol: IrClassSymbol? = null,
    val isFinal: Boolean = true,
    val isSynthetic: Boolean = false,
    val isOverriding: Boolean = true,
    // 'true' if we also should produce a synthetic bridge with unsubstituted signature.
    // NB this is passed down the hierarchy to the point where 'unsubstitutedSpecialBridge' is created,
    // see BridgeLoweringCache::computeSpecialBridge
    val needsUnsubstitutedBridge: Boolean = false,
    val unsubstitutedSpecialBridge: SpecialBridge? = null
)
