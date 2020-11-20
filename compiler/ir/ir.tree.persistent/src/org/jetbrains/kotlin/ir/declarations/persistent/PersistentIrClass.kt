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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.ClassCarrier
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import java.util.*

internal class PersistentIrClass(
    override val startOffset: Int,
    override val endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val name: Name,
    override val kind: ClassKind,
    visibility: DescriptorVisibility,
    modality: Modality,
    override val isCompanion: Boolean = false,
    override val isInner: Boolean = false,
    override val isData: Boolean = false,
    override val isExternal: Boolean = false,
    override val isInline: Boolean = false,
    override val isExpect: Boolean = false,
    override val isFun: Boolean = false,
    override val source: SourceElement = SourceElement.NO_SOURCE
) : IrClass(),
    PersistentIrDeclarationBase<ClassCarrier>,
    ClassCarrier {

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
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var visibilityField: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() = getCarrier().visibilityField
        set(v) {
            if (visibility !== v) {
                setCarrier().visibilityField = v
            }
        }

    override var thisReceiverField: IrValueParameter? = null

    override var thisReceiver: IrValueParameter?
        get() = getCarrier().thisReceiverField
        set(v) {
            if (thisReceiver !== v) {
                setCarrier().thisReceiverField = v
            }
        }

    private var initialDeclarations: MutableList<IrDeclaration>? = null

    override val declarations: MutableList<IrDeclaration> = ArrayList()
        get() {
            if (createdOn < stageController.currentStage && initialDeclarations == null) {
                initialDeclarations = Collections.unmodifiableList(ArrayList(field))
            }

            return if (stageController.canAccessDeclarationsOf(this)) {
                ensureLowered()
                field
            } else {
                initialDeclarations ?: field
            }
        }

    override var typeParametersField: List<IrTypeParameter> = emptyList()

    override var typeParameters: List<IrTypeParameter>
        get() = getCarrier().typeParametersField
        set(v) {
            if (typeParameters !== v) {
                setCarrier().typeParametersField = v
            }
        }

    override var superTypesField: List<IrType> = emptyList()

    override var superTypes: List<IrType>
        get() = getCarrier().superTypesField
        set(v) {
            if (superTypes !== v) {
                setCarrier().superTypesField = v
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

    override var modalityField: Modality = modality

    override var modality: Modality
        get() = getCarrier().modalityField
        set(v) {
            if (modality !== v) {
                setCarrier().modalityField = v
            }
        }

    override var attributeOwnerIdField: IrAttributeContainer = this

    override var attributeOwnerId: IrAttributeContainer
        get() = getCarrier().attributeOwnerIdField
        set(v) {
            if (attributeOwnerId !== v) {
                setCarrier().attributeOwnerIdField = v
            }
        }
}
