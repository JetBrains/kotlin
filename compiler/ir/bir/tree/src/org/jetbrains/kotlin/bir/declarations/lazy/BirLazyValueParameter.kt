/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.name.Name

class BirLazyValueParameter(
    override val originalElement: IrValueParameter,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirValueParameter {
    override val owner: BirValueParameter
        get() = this

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ParameterDescriptor
        get() = originalElement.descriptor
    override var index: Int
        get() = originalElement.index
        set(value) = mutationNotSupported()
    override var isCrossinline: Boolean
        get() = originalElement.isCrossinline
        set(value) = mutationNotSupported()
    override var isNoinline: Boolean
        get() = originalElement.isNoinline
        set(value) = mutationNotSupported()
    override var isHidden: Boolean
        get() = originalElement.isHidden
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalElement.name
        set(value) = mutationNotSupported()
    override var isAssignable: Boolean
        get() = originalElement.isAssignable
        set(value) = mutationNotSupported()
    override var type: BirType by lazyVar<BirLazyValueParameter, _> {
        converter.remapType(originalElement.type)
    }
    override var varargElementType: BirType? by lazyVar<BirLazyValueParameter, _> {
        converter.remapType(originalElement.varargElementType)
    }
    override var defaultValue: BirExpressionBody? by lazyVar<BirLazyValueParameter, _> {
        convertChild(originalElement.defaultValue)
    }
    override val annotations = lazyChildElementList<BirLazyValueParameter, BirConstructorCall>(1) { originalElement.annotations }
}