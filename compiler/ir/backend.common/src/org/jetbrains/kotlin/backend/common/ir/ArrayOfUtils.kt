/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

private val PRIMITIVE_NAMES: List<String> =
    PrimitiveType.entries.map { it.typeName.asString() } + UnsignedType.entries.map { it.typeName.asString() }

private val PRIMITIVE_ARRAY_NAMES: Set<String> =
    PRIMITIVE_NAMES.map { it + "Array" }.toSet()

private val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
    PRIMITIVE_NAMES.map { name -> name.toLowerCaseAsciiOnly() + "ArrayOf" }.toSet()

private const val ARRAY_NAME = "Array"
private const val ARRAY_OF_NAME = "arrayOf"

fun IrFunction.isArrayOfOrArrayDotOf(): Boolean = isArrayOf() || isArrayDotOf()

private fun IrFunction.isArrayOf(): Boolean {
    val parent = when (val directParent = parent) {
        is IrClass -> directParent.getPackageFragment()
        is IrPackageFragment -> directParent
        else -> return false
    }
    return parent.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
            name.asString().let { it in PRIMITIVE_ARRAY_OF_NAMES || it == ARRAY_OF_NAME } &&
            hasSingleVarargParameter()
}

private fun IrFunction.isArrayDotOf(): Boolean {
    val klass = parent as? IrClass ?: return false
    return name == OperatorNameConventions.OF &&
            klass.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
            klass.name.asString().let { it in PRIMITIVE_ARRAY_NAMES || it == ARRAY_NAME } &&
            hasSingleVarargParameter()
}

private fun IrFunction.hasSingleVarargParameter(): Boolean =
    hasShape(regularParameters = 1) && parameters[0].isVararg
