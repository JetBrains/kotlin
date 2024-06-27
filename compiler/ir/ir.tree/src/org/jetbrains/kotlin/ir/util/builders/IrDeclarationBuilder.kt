/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.copyAnnotations
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.Name

abstract class IrDeclarationBuilder<D: IrDeclaration> {
    var name: Name
    var startOffset: Int
    var endOffset: Int
    var origin: IrDeclarationOrigin
    var annotations: MutableList<IrConstructorCall>

    @PublishedApi internal constructor(name: Name) {
        this.name = name
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
        origin = IrDeclarationOrigin.DEFINED
        annotations = mutableListOf()
    }
    @PublishedApi internal constructor(name: Name, from: D) {
        this.name = name
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        annotations = from.annotations.mapTo(mutableListOf()) { it.deepCopyWithSymbols() }
    }
}

abstract class IrDeclarationWithVisibilityBuilder<D: IrDeclarationWithVisibility> : IrDeclarationBuilder<D> {
    var visibility: DescriptorVisibility

    @PublishedApi internal constructor(name: Name) : super(name) {
        visibility = DescriptorVisibilities.PUBLIC
    }
    @PublishedApi internal constructor(name: Name, from: D) : super(name, from) {
        visibility = from.visibility
    }
}