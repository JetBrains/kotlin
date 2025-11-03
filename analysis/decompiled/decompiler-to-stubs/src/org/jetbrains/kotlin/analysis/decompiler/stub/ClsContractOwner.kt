/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.receiverType

sealed class ClsContractOwner {
    abstract val contract: ProtoBuf.Contract

    abstract fun receiverType(typeTable: TypeTable): ProtoBuf.Type?

    abstract val valueParameterCount: Int
    abstract val valueParameters: List<ProtoBuf.ValueParameter>
    abstract val contextParameters: List<ProtoBuf.ValueParameter>

    class Function(val functionProto: ProtoBuf.Function) : ClsContractOwner() {
        override val contract: ProtoBuf.Contract
            get() = functionProto.contract

        override fun receiverType(typeTable: TypeTable): ProtoBuf.Type? = functionProto.receiverType(typeTable)

        override val valueParameterCount: Int
            get() = functionProto.valueParameterCount

        override val valueParameters: List<ProtoBuf.ValueParameter>
            get() = functionProto.valueParameterList

        override val contextParameters: List<ProtoBuf.ValueParameter>
            get() = functionProto.contextParameterList
    }

    class PropertyAccessor(val propertyProto: ProtoBuf.Property, val isGetter: Boolean) : ClsContractOwner() {
        override val contract: ProtoBuf.Contract
            get() = if (isGetter) propertyProto.getterContract!! else propertyProto.setterContract!!

        override fun receiverType(typeTable: TypeTable): ProtoBuf.Type? = propertyProto.receiverType(typeTable)

        override val valueParameterCount: Int
            get() = if (isGetter) 0 else 1

        override val contextParameters: List<ProtoBuf.ValueParameter>
            get() = propertyProto.contextParameterList

        override val valueParameters: List<ProtoBuf.ValueParameter>
            get() = if (isGetter) emptyList() else listOf(propertyProto.setterValueParameter)
    }
}