/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.IrElementBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.name.Name

abstract class IrDeclarationBuilder : IrElementBuilder() {

    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    var visibility: Visibility = Visibilities.PUBLIC

    lateinit var name: Name

    fun updateFrom(from: IrDeclaration) {
        super.updateFrom(from)

        origin = from.origin
        visibility = if (from is IrDeclarationWithVisibility) from.visibility else Visibilities.PUBLIC
    }

}
