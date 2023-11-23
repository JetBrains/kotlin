/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.lazy.acceptLiteIfPresent
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.name.Name

class BirLazyTypeAlias(
    override val originalIrElement: IrTypeAlias,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirTypeAlias {
    override val owner: BirTypeAlias
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: TypeAliasDescriptor
        get() = originalIrElement.descriptor
    override var isActual: Boolean
        get() = originalIrElement.isActual
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalIrElement.visibility
        set(value) = mutationNotSupported()
    override var expandedType by lazyVar<BirLazyTypeAlias, _> { converter.remapType(originalIrElement.expandedType) }
    override val typeParameters = lazyChildElementList<BirLazyTypeAlias, BirTypeParameter>(1) { originalIrElement.typeParameters }
    override val annotations = lazyChildElementList<BirLazyTypeAlias, BirConstructorCall>(2) { originalIrElement.annotations }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
    }
}