/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
val kotlinReflectionPackageFqn = kotlinPackageFqn.child(Name.identifier("reflect"))

fun IrType.isFunction(): Boolean {
    val classifier = classifierOrNull ?: return false
    val name = classifier.descriptor.name.asString()
    if (!name.startsWith("Function")) return false
    val declaration = classifier.owner as IrDeclaration
    val parent = declaration.parent as? IrPackageFragment ?: return false

    return parent.fqName == kotlinPackageFqn
}


fun IrType.isKFunction(): Boolean {
    val classifier = classifierOrNull ?: return false
    val name = classifier.descriptor.name.asString()
    if (!name.startsWith("KFunction")) return false
    val declaration = classifier.owner as IrDeclaration
    val parent = declaration.parent as? IrPackageFragment ?: return false

    return parent.fqName == kotlinReflectionPackageFqn
}


fun IrType.superTypes(): List<IrType> {
    val classifier = classifierOrNull?.owner ?: return emptyList()
    return when(classifier) {
        is IrClass -> classifier.superTypes
        is IrTypeParameter -> classifier.superTypes
        else -> throw IllegalStateException()
    }
}

fun IrType.typeParameterSuperTypes(): List<IrType> {
    val classifier = classifierOrNull ?: return emptyList()
    return when(classifier) {
        is IrTypeParameterSymbol -> classifier.owner.superTypes
        is IrClassSymbol -> emptyList()
        else -> throw IllegalStateException()
    }
}

fun IrType.isFunctionTypeOrSubtype(): Boolean = DFS.ifAny(listOf(this), { it.superTypes() }, { it.isFunction() })

fun IrType.isTypeParameter() = classifierOrNull is IrTypeParameterSymbol

fun IrType.isInterface() = (classifierOrNull?.owner as? IrClass)?.kind == ClassKind.INTERFACE

fun IrType.isFunctionOrKFunction() = isFunction() || isKFunction()

fun IrType.isNullable(): Boolean = DFS.ifAny(listOf(this), { it.typeParameterSuperTypes() }, {
    when (it) {
        is IrSimpleType -> it.hasQuestionMark
        else -> it is IrDynamicType
    }
})


fun IrType.isThrowable(): Boolean {
    if (this is IrSimpleType) {
        val classClassifier = classifier as? IrClassSymbol ?: return false
        if (classClassifier.owner.name.asString() != "Throwable") return false
        val parent = classClassifier.owner.parent as? IrPackageFragment ?: return false
        return parent.fqName == kotlinPackageFqn
    } else return false
}

fun IrType.isThrowableTypeOrSubtype() = DFS.ifAny(listOf(this), IrType::superTypes, IrType::isThrowable)