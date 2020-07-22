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

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.FunctionBaseCarrier
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.name.Name

abstract class IrFunctionBase<T : FunctionBaseCarrier>(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val name: Name,
    visibility: Visibility,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    returnType: IrType
) :
    IrDeclarationBase<T>(startOffset, endOffset, origin),
    IrFunction,
    FunctionBaseCarrier {

    override var returnTypeFieldField: IrType = returnType

    private var returnTypeField: IrType
        get() = getCarrier().returnTypeFieldField
        set(v) {
            if (returnTypeField !== v) {
                setCarrier().returnTypeFieldField = v
            }
        }

    @Suppress("DEPRECATION")
    final override var returnType: IrType
        get() = returnTypeField.let {
            if (it !== IrUninitializedType) it else error("Return type is not initialized")
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

    final override var body: IrBody?
        get() = getCarrier().bodyField
        set(v) {
            if (body !== v) {
                if (v is IrBodyBase<*>) {
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

    override var visibilityField: Visibility = visibility

    override var visibility: Visibility
        get() = getCarrier().visibilityField
        set(v) {
            if (visibility !== v) {
                setCarrier().visibilityField = v
            }
        }
}
