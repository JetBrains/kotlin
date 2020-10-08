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

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ConstructorCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal class PersistentIrConstructor(
    override val startOffset: Int,
    override val endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    override val name: Name,
    visibility: DescriptorVisibility,
    returnType: IrType,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isPrimary: Boolean,
    override val isExpect: Boolean,
    override val containerSource: DeserializedContainerSource?
) : IrConstructor(),
    PersistentIrDeclarationBase<ConstructorCarrier>,
    ConstructorCarrier {

    init {
        symbol.bind(this)
    }

    override var lastModified: Int = stageController.currentStage
    override var loweredUpTo: Int = stageController.currentStage
    override var values: Array<Carrier>? = null
    override val createdOn: Int = stageController.currentStage

    override var parentField: IrDeclarationParent? = null
    override var originField: IrDeclarationOrigin = origin
    override var removedOn: Int = Int.MAX_VALUE
    override var annotationsField: List<IrConstructorCall> = emptyList()

    override var returnTypeFieldField: IrType = returnType

    private var returnTypeField: IrType
        get() = getCarrier().returnTypeFieldField
        set(v) {
            if (returnTypeField !== v) {
                setCarrier().returnTypeFieldField = v
            }
        }

    override var returnType: IrType
        get() = returnTypeField.let {
            if (it !== IrUninitializedType) it else throw ReturnTypeIsNotInitializedException(this)
        }
        set(c) {
            returnTypeField = c
        }

    override var typeParametersField: List<IrTypeParameter> = emptyList()

    override var typeParameters: List<IrTypeParameter>
        get() = getCarrier().typeParametersField
        set(v) {
            if (typeParameters !== v) {
                setCarrier().typeParametersField = v
            }
        }

    override var dispatchReceiverParameterField: IrValueParameter? = null

    override var dispatchReceiverParameter: IrValueParameter?
        get() = getCarrier().dispatchReceiverParameterField
        set(v) {
            if (dispatchReceiverParameter !== v) {
                setCarrier().dispatchReceiverParameterField = v
            }
        }

    override var extensionReceiverParameterField: IrValueParameter? = null

    override var extensionReceiverParameter: IrValueParameter?
        get() = getCarrier().extensionReceiverParameterField
        set(v) {
            if (extensionReceiverParameter !== v) {
                setCarrier().extensionReceiverParameterField = v
            }
        }

    override var valueParametersField: List<IrValueParameter> = emptyList()

    override var valueParameters: List<IrValueParameter>
        get() = getCarrier().valueParametersField
        set(v) {
            if (valueParameters !== v) {
                setCarrier().valueParametersField = v
            }
        }

    override var bodyField: IrBody? = null

    override var body: IrBody?
        get() = getCarrier().bodyField
        set(v) {
            if (body !== v) {
                if (v is PersistentIrBodyBase<*>) {
                    v.container = this
                }
                setCarrier().bodyField = v
            }
        }

    override var metadataField: MetadataSource? = null

    override var metadata: MetadataSource?
        get() = getCarrier().metadataField
        set(v) {
            if (metadata !== v) {
                setCarrier().metadataField = v
            }
        }

    override var visibilityField: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() = getCarrier().visibilityField
        set(v) {
            if (visibility !== v) {
                setCarrier().visibilityField = v
            }
        }

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = symbol.descriptor
}
