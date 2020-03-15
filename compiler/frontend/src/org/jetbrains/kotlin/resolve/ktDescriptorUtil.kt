/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.contains

fun FunctionDescriptor.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnType ?: return false
    if (returnType is DeferredType && returnType.isComputing) return false

    if (returnType.constructor != typeParameter.typeConstructor) return false

    fun KotlinType.isBadType() = contains { it.constructor == typeParameter.typeConstructor }

    if (valueParameters.any { it.type.isBadType() } || extensionReceiverParameter?.type?.isBadType() == true) return false

    return true
}

internal fun CallableMemberDescriptor.isEffectivelyFinal(ignoreEnumClassFinality: Boolean): Boolean =
    modality == Modality.FINAL ||
            containingDeclaration.let { parent ->
                (ignoreEnumClassFinality || !DescriptorUtils.isEnumClass(parent)) &&
                        parent is ClassDescriptor && parent.modality == Modality.FINAL
            }

fun ParameterDescriptor.indexOrMinusOne(): Int =
    when (this) {
        is ReceiverParameterDescriptor -> -1
        is ValueParameterDescriptor -> index
        else -> error("expected either receiver or value parameter, but got: $this")
    }
