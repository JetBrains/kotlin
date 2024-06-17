/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.name.Name

abstract class IrDeclarationBuilder<D: IrDeclaration> {
    var name: Name
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED

    @PublishedApi internal constructor(name: Name) {
        this.name = name
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
        origin = IrDeclarationOrigin.DEFINED
    }
    @PublishedApi internal constructor(name: Name, from: D) {
        this.name = name
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
    }
}

abstract class IrDeclarationWithVisibilityBuilder<D: IrDeclarationWithVisibility> : IrDeclarationBuilder<D> {
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    @PublishedApi internal constructor(name: Name) : super(name)
    @PublishedApi internal constructor(name: Name, from: D) : super(name, from) {
        visibility = from.visibility
    }
}