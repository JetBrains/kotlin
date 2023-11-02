/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.Name

class BirLazyClass(
    override val originalElement: IrClass,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirClass {
    override val owner: BirClass
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ClassDescriptor
        get() = originalElement.descriptor
    override var kind: ClassKind
        get() = originalElement.kind
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalElement.modality
        set(value) = mutationNotSupported()
    override var isCompanion: Boolean
        get() = originalElement.isCompanion
        set(value) = mutationNotSupported()
    override var isInner: Boolean
        get() = originalElement.isInner
        set(value) = mutationNotSupported()
    override var isData: Boolean
        get() = originalElement.isData
        set(value) = mutationNotSupported()
    override var isValue: Boolean
        get() = originalElement.isValue
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalElement.isExpect
        set(value) = mutationNotSupported()
    override var isFun: Boolean
        get() = originalElement.isFun
        set(value) = mutationNotSupported()
    override var hasEnumEntries: Boolean
        get() = originalElement.hasEnumEntries
        set(value) = mutationNotSupported()
    override val source: SourceElement
        get() = originalElement.source
    override var name: Name
        get() = originalElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalElement.visibility
        set(value) = mutationNotSupported()
    override var isExternal: Boolean
        get() = originalElement.isExternal
        set(value) = mutationNotSupported()
    override var valueClassRepresentation: ValueClassRepresentation<BirSimpleType>? by lazyVar<BirLazyClass, _> {
        originalElement.valueClassRepresentation?.mapUnderlyingType { converter.remapSimpleType(it) }
    }
    override var superTypes: List<BirType> by lazyVar<BirLazyClass, _> {
        originalElement.superTypes.map { converter.remapType(it) }
    }
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazyClass, _> {
        converter.remapElement(originalElement.attributeOwnerId)
    }
    override var thisReceiver: BirValueParameter? by lazyVar<BirLazyClass, _> {
        convertChild(originalElement.thisReceiver)
    }
    override val typeParameters = lazyChildElementList<BirLazyClass, BirTypeParameter>(1) { originalElement.typeParameters }
    override val annotations = lazyChildElementList<BirLazyClass, BirConstructorCall>(2) { originalElement.annotations }
    override val declarations = lazyChildElementList<BirLazyClass, BirDeclaration>(3) { originalElement.declarations }
}