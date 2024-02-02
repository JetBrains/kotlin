/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.Name

class BirLazyClass(
    override val originalIrElement: IrClass,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirClass, BirClassSymbol {
    override val owner: BirClass
        get() = this
    override val symbol: BirClassSymbol
        get() = this

    override var kind: ClassKind
        get() = originalIrElement.kind
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalIrElement.modality
        set(value) = mutationNotSupported()
    override var isCompanion: Boolean
        get() = originalIrElement.isCompanion
        set(value) = mutationNotSupported()
    override var isInner: Boolean
        get() = originalIrElement.isInner
        set(value) = mutationNotSupported()
    override var isData: Boolean
        get() = originalIrElement.isData
        set(value) = mutationNotSupported()
    override var isValue: Boolean
        get() = originalIrElement.isValue
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalIrElement.isExpect
        set(value) = mutationNotSupported()
    override var isFun: Boolean
        get() = originalIrElement.isFun
        set(value) = mutationNotSupported()
    override var hasEnumEntries: Boolean
        get() = originalIrElement.hasEnumEntries
        set(value) = mutationNotSupported()
    override val source: SourceElement
        get() = originalIrElement.source
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalIrElement.visibility
        set(value) = mutationNotSupported()
    override var isExternal: Boolean
        get() = originalIrElement.isExternal
        set(value) = mutationNotSupported()
    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? by lazyVar<BirLazyClass, _> {
        originalIrElement.valueClassRepresentation?.mapUnderlyingType { converter.remapSimpleType(it) }
    }
    override var superTypes: List<BirType> by lazyVar<BirLazyClass, _> {
        originalIrElement.superTypes.map { converter.remapType(it) }
    }
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazyClass, _> {
        converter.remapElement(originalIrElement.attributeOwnerId)
    }
    private val _thisReceiver = lazyVar<BirLazyClass, _> {
        convertChild<BirValueParameter?>(originalIrElement.thisReceiver)
    }
    override var thisReceiver: BirValueParameter? by _thisReceiver
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazyClass, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
    override val typeParameters = lazyChildElementList<BirLazyClass, BirTypeParameter>(1) { originalIrElement.typeParameters }
    override val declarations = lazyChildElementList<BirLazyClass, BirDeclaration>(3) { originalIrElement.declarations }
}