/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
val kotlinReflectionPackageFqn = kotlinPackageFqn.child(Name.identifier("reflect"))
val kotlinCoroutinesPackageFqn = kotlinPackageFqn.child(Name.identifier("coroutines"))


fun IrType.isFunction() = this.isNameInPackage("Function", kotlinPackageFqn)
fun IrType.isKClass() = this.isNameInPackage("KClass", kotlinReflectionPackageFqn)
fun IrType.isKFunction() = this.isNameInPackage("KFunction", kotlinReflectionPackageFqn)
fun IrType.isSuspendFunction() = this.isNameInPackage("SuspendFunction", kotlinCoroutinesPackageFqn)

fun IrType.isNameInPackage(prefix: String, packageFqName: FqName): Boolean {
    val classifier = classifierOrNull ?: return false
    val name = classifier.descriptor.name.asString()
    if (!name.startsWith(prefix)) return false
    val declaration = classifier.owner as IrDeclaration
    val parent = declaration.parent as? IrPackageFragment ?: return false

    return parent.fqName == packageFqName

}

fun IrType.superTypes() = classifierOrNull?.superTypes() ?: emptyList()

fun IrType.typeParameterSuperTypes(): List<IrType> {
    val classifier = classifierOrNull ?: return emptyList()
    return when (classifier) {
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


fun IrType.isThrowable(): Boolean = isTypeFromKotlinPackage { name -> name.asString() == "Throwable" }

fun IrType.isThrowableTypeOrSubtype() = DFS.ifAny(listOf(this), IrType::superTypes, IrType::isThrowable)

fun IrType.isUnsigned(): Boolean = isTypeFromKotlinPackage { name -> UnsignedTypes.isShortNameOfUnsignedType(name) }

private inline fun IrType.isTypeFromKotlinPackage(namePredicate: (Name) -> Boolean): Boolean {
    if (this is IrSimpleType) {
        val classClassifier = classifier as? IrClassSymbol ?: return false
        if (!namePredicate(classClassifier.owner.name)) return false
        val parent = classClassifier.owner.parent as? IrPackageFragment ?: return false
        return parent.fqName == kotlinPackageFqn
    } else return false
}

fun IrType.isPrimitiveArray() = isTypeFromKotlinPackage { it in FQ_NAMES.primitiveArrayTypeShortNames }

fun IrType.getPrimitiveArrayElementType() = (this as? IrSimpleType)?.let {
    (it.classifier.owner as? IrClass)?.fqNameWhenAvailable?.toUnsafe()?.let { fqn -> FQ_NAMES.arrayClassFqNameToPrimitiveType[fqn] }
}

fun IrType.isNonPrimitiveArray() =
    (this.isArray() || this.isNullableArray()) && !this.isPrimitiveArray()
