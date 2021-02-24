/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

// TODO: could be a lowering
internal class ExternalPackageParentPatcher(private val stubGenerator: DeclarationStubGenerator) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
        super.visitMemberAccess(expression)
        val callee = expression.symbol.owner as? IrDeclaration ?: return
        if (callee.parent is IrExternalPackageFragment) {
            @OptIn(ObsoleteDescriptorBasedAPI::class)
            val parentClass = stubGenerator.generateOrGetFacadeClass(callee.descriptor) ?: return
            callee.parent = parentClass
            when (callee) {
                is IrProperty -> handleProperty(callee, parentClass)
                is IrSimpleFunction -> callee.correspondingPropertySymbol?.owner?.let { handleProperty(it, parentClass) }
            }
        }
    }

    private fun handleProperty(property: IrProperty, newParent: IrClass) {
        property.parent = newParent
        property.getter?.parent = newParent
        property.setter?.parent = newParent
        property.backingField?.parent = newParent
    }
}