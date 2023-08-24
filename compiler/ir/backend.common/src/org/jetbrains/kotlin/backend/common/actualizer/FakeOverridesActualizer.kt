/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * It actualizes expect fake overrides in non-expect classes inside common or multi-platform module.
 *
 * When some non-expect class inside a common or multi-platform module has an expect base class,
 * FIR2IR generates expect fake overrides overriding the expect base class members for this non-expect class which is not correct for the backend.
 * This Actualizer processes expect overridable declarations in non-expect classes and replaces them with the associated actual overridable
 * declarations overriding the actual base class members.The newly created actual fake overrides are stored in expectActualMap.
 */
internal class FakeOverridesActualizer(private val expectActualMap: MutableMap<IrSymbol, IrSymbol>) : IrElementVisitorVoid {
    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect) {
            actualizeFakeOverrides(declaration)
        }
        visitElement(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun actualizeFakeOverrides(klass: IrClass) {
        fun IrDeclaration.actualize(): IrDeclaration {
            if (!isExpect) return this

            (expectActualMap[symbol]?.owner as? IrDeclaration)?.let { return it }

            require(this is IrOverridableDeclaration<*>)
            val actualizedOverrides = overriddenSymbols.map { (it.owner as IrDeclaration).actualize() }
            val actualFakeOverride = createFakeOverrideMember(actualizedOverrides, parent as IrClass)

            recordActualForExpectDeclaration(this.symbol, actualFakeOverride.symbol, expectActualMap)

            return actualFakeOverride
        }

        klass.declarations.transformInPlace { if (it.isExpect && it.isFakeOverride) it.actualize() else it }
    }
}
