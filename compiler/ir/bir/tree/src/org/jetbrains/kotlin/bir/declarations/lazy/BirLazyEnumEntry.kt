/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.lazy.acceptLiteIfPresent
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.name.Name

class BirLazyEnumEntry(
    override val originalIrElement: IrEnumEntry,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirEnumEntry {
    override val owner: BirEnumEntry
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ClassDescriptor
        get() = originalIrElement.descriptor
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override val annotations = lazyChildElementList<BirLazyEnumEntry, BirConstructorCall>(1) { originalIrElement.annotations }
    private val _correspondingClass =  lazyVar<BirLazyEnumEntry, _> {
        converter.remapElement<BirClass>(originalIrElement.correspondingClass)
    }
    override var correspondingClass: BirClass? by _correspondingClass
    private val _initializerExpression = lazyVar<BirLazyEnumEntry, _> {
        convertChild<BirExpressionBody?>(originalIrElement.initializerExpression)
    }
    override var initializerExpression: BirExpressionBody? by _initializerExpression

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        _initializerExpression.acceptLiteIfPresent(visitor)
        _correspondingClass.acceptLiteIfPresent(visitor)
    }
}