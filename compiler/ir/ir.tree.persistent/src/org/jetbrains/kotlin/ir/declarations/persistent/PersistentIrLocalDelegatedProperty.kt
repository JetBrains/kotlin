/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    override var getterField: IrSimpleFunction? = null

    override var getter: IrSimpleFunction
        get() = getCarrier().getterField!!
        set(v) {
            if (getCarrier().getterField !== v) {
                setCarrier().getterField = v
            }
        }

    override var setterField: IrSimpleFunction? = null

    override var setter: IrSimpleFunction?
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
