/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.DFS


fun IrType.isFunctionTypeOrSubtype(): Boolean {

    val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
    fun checkType(irType: IrType): Boolean {
        val classifier = irType.classifierOrNull ?: return false
        val name = classifier.descriptor.name.asString()
        if (!name.startsWith("Function")) return false
        val declaration = classifier.owner as IrDeclaration
        val parent = declaration.parent as? IrPackageFragment ?: return false

        return parent.fqName == kotlinPackageFqn
    }

    fun superTypes(irType: IrType): List<IrType> {
        val classifier = irType.classifierOrNull?.owner ?: return emptyList()
        return when(classifier) {
            is IrClass -> classifier.superTypes
            is IrTypeParameter -> classifier.superTypes
            else -> throw IllegalStateException()
        }
    }

    return DFS.ifAny(listOf(this), ::superTypes, ::checkType)
}