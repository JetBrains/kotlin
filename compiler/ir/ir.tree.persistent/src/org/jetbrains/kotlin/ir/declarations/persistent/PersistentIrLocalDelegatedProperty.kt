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

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.LocalDelegatedPropertyCarrier
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

// TODO make not persistent
internal class PersistentIrLocalDelegatedProperty(
    override val startOffset: Int,
    override val endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override val name: Name,
    type: IrType,
    override val isVar: Boolean
) :
    PersistentIrDeclarationBase<LocalDelegatedPropertyCarrier>,
    IrLocalDelegatedProperty(),
    LocalDelegatedPropertyCarrier {

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

    @ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptorWithAccessors
        get() = symbol.descriptor

    override var typeField: IrType = type

    override var type: IrType
        get() = getCarrier().typeField
        set(v) {
            if (getCarrier().typeField !== v) {
                setCarrier().typeField = v
            }
        }

    override var delegateField: IrVariable? = null

    override var delegate: IrVariable
        get() = getCarrier().delegateField!!
        set(v) {
            if (getCarrier().delegateField !== v) {
                setCarrier().delegateField = v
            }
        }

    override var getterField: IrFunction? = null

    override var getter: IrFunction
        get() = getCarrier().getterField!!
        set(v) {
            if (getCarrier().getterField !== v) {
                setCarrier().getterField = v
            }
        }

    override var setterField: IrFunction? = null

    override var setter: IrFunction?
        get() = getCarrier().setterField
        set(v) {
            if (setter !== v) {
                setCarrier().setterField = v
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
}
