/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.util.isLocalClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

interface IrClassifierEqualityChecker {
    fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean

    fun getHashCode(symbol: IrClassifierSymbol): Int
}

@OptIn(DescriptorBasedIr::class)
object FqNameEqualityChecker : IrClassifierEqualityChecker {
    override fun areEqual(left: IrClassifierSymbol, right: IrClassifierSymbol): Boolean {
        if (left === right) return true
        if (!left.isBound || !right.isBound) {
            return checkViaDescriptors(left.descriptor, right.descriptor)
        }
        return checkViaDeclarations(left.owner, right.owner)
    }

    override fun getHashCode(symbol: IrClassifierSymbol): Int {
        if (symbol.isBound) {
            val owner = symbol.owner
            if (owner is IrClass && !owner.isLocalClass()) {
                return owner.fqName.hashCode()
            }
            return owner.hashCode()
        }

        val descriptor = symbol.descriptor
        if (descriptor is ClassDescriptor && !DescriptorUtils.isLocal(descriptor)) {
            return descriptor.fqNameSafe.hashCode()
        }
        return descriptor.hashCode()
    }

    private val IrDeclarationWithName.fqName: FqName?
        get() {
            val parentFqName = when (val parent = parent) {
                is IrPackageFragment -> parent.fqName
                is IrDeclarationWithName -> parent.fqName
                else -> return null
            }
            return parentFqName?.child(name)
        }

    private fun checkViaDeclarations(c1: IrSymbolOwner, c2: IrSymbolOwner): Boolean {
        if (c1 is IrClass && c2 is IrClass) {
            if (c1.isLocalClass() || c2.isLocalClass())
                return c1 === c2 // Local declarations should be identical

            return c1.fqName == c2.fqName
        }

        return c1 == c2
    }

    private fun checkViaDescriptors(c1: ClassifierDescriptor, c2: ClassifierDescriptor) = c1.typeConstructor == c2.typeConstructor
}
