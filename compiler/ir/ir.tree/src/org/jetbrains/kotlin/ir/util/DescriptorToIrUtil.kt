/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.types.KotlinType

val ParameterDescriptor.indexOrMinusOne: Int
    get() = if (this is ValueParameterDescriptor) index else -1

val ParameterDescriptor.varargElementType: KotlinType?
    get() = (this as? ValueParameterDescriptor)?.varargElementType

val ParameterDescriptor.isCrossinline: Boolean
    get() = this is ValueParameterDescriptor && isCrossinline

val ParameterDescriptor.isNoinline: Boolean
    get() = this is ValueParameterDescriptor && isNoinline

fun IrFactory.createIrClassFromDescriptor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrClassSymbol,
        descriptor: ClassDescriptor,
        name: Name = descriptor.name,
        visibility: DescriptorVisibility = descriptor.visibility,
        modality: Modality = descriptor.modality
): IrClass = createClass(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    name = name,
    visibility = visibility,
    symbol = symbol,
    kind = descriptor.kind,
    modality = modality,
    isExternal = descriptor.isEffectivelyExternal(),
    isCompanion = descriptor.isCompanionObject,
    isInner = descriptor.isInner,
    isData = descriptor.isData,
    isValue = descriptor.isValueClass(),
    isExpect = descriptor.isExpect,
    isFun = descriptor.isFun,
    source = descriptor.source,
)
