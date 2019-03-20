/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.FqName

interface IrClassifierEqualityChecker {
    fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean
}

object FqNameEqualityChecker : IrClassifierEqualityChecker {
    override fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean {
        if (left === right) return true
        if (!left.isBound || !right.isBound) checkViaDescriptors(left.descriptor, right.descriptor)
        return checkViaDeclarations(left.owner, right.owner)
    }

    private val IrDeclarationWithName.fqName
        get(): FqName? {
            val parentFqName = when (val parent = parent) {
                is IrPackageFragment -> parent.fqName
                is IrDeclarationWithName -> parent.fqName
                else -> return null
            }
            return parentFqName?.child(name)
        }

    private fun isLocalClass(declaration: IrClass): Boolean {
        var current: IrDeclarationParent? = declaration
        while (current != null && current !is IrPackageFragment) {
            if (current is IrDeclarationWithVisibility && current.visibility == Visibilities.LOCAL)
                return true
            current = (current as? IrDeclaration)?.parent
        }

        return false
    }

    private fun checkViaDeclarations(c1: IrSymbolOwner, c2: IrSymbolOwner): Boolean {
        if (c1 is IrClass && c2 is IrClass) {
            if (isLocalClass(c1) || isLocalClass(c2))
                return c1 === c2 // Local declarations should be identical

            return c1.fqName == c2.fqName
        }

        return c1 == c2
    }

    private fun checkViaDescriptors(c1: ClassifierDescriptor, c2: ClassifierDescriptor) = c1.typeConstructor == c2.typeConstructor
}