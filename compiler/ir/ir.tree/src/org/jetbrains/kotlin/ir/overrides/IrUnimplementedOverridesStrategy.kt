/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember

interface IrUnimplementedOverridesStrategy {
    class Customization(val origin: IrDeclarationOrigin?, val modality: Modality?) {
        companion object {
            val NO = Customization(null, null)
        }
    }

    fun <T : IrOverridableMember> computeCustomization(overridableMember: T, parent: IrClass): Customization

    object ProcessAsFakeOverrides : IrUnimplementedOverridesStrategy {
        override fun <T : IrOverridableMember> computeCustomization(overridableMember: T, parent: IrClass) = Customization.NO
    }
}
