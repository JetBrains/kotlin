/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrConstructorImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    returnType: IrType,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isPrimary: Boolean,
    override val isExpect: Boolean,
    override val containerSource: DeserializedContainerSource? = null,
    override val factory: IrFactory = IrFactoryImpl,
) : IrConstructor() {
    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var returnType: IrType = returnType
        get() = if (field === IrUninitializedType) {
            throw ReturnTypeIsNotInitializedException(this)
        } else {
            field
        }

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var dispatchReceiverParameter: IrValueParameter? = null
    override var extensionReceiverParameter: IrValueParameter? = null
    override var contextReceiverParametersCount: Int = 0
    override var valueParameters: List<IrValueParameter> = emptyList()

    override var body: IrBody? = null

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = symbol.descriptor
}
