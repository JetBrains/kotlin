/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.ir.types.isNullable as irTreeTypeUtils_isNullable

val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
val kotlinReflectionPackageFqn = kotlinPackageFqn.child(Name.identifier("reflect"))
val kotlinCoroutinesPackageFqn = kotlinPackageFqn.child(Name.identifier("coroutines"))


fun IrType.isFunction() = this.isNameInPackage("Function", kotlinPackageFqn)
fun IrType.isKClass() = this.isNameInPackage("KClass", kotlinReflectionPackageFqn)
fun IrType.isKFunction() = this.isNameInPackage("KFunction", kotlinReflectionPackageFqn)
fun IrType.isSuspendFunction() = this.isNameInPackage("SuspendFunction", kotlinCoroutinesPackageFqn)

fun IrType.isKotlinResult(): Boolean = isNameInPackage("Result", kotlinPackageFqn)
fun IrType.isContinuation(): Boolean = isNameInPackage("Continuation", kotlinCoroutinesPackageFqn)
fun IrType.isNullableContinuation(): Boolean = isContinuation() && this is IrSimpleType && hasQuestionMark

fun IrType.isKClassArray(): Boolean {
    if (!isNonPrimitiveArray()) return false
    val argument = (this as? IrSimpleType)?.arguments?.singleOrNull() ?: return false
    val argumentType = (argument as? IrTypeProjection)?.type ?: return false
    return argumentType.isKClass()
}

fun IrType.isNameInPackage(prefix: String, packageFqName: FqName): Boolean {
    val classifier = classifierOrNull ?: return false
    val name = classifier.descriptor.name.asString()
    if (!name.startsWith(prefix)) return false
    val declaration = classifier.owner as IrDeclaration
    val parent = declaration.parent as? IrPackageFragment ?: return false

    return parent.fqName == packageFqName

}

fun IrType.superTypes() = classifierOrNull?.superTypes() ?: emptyList()

fun IrType.isFunctionTypeOrSubtype(): Boolean = DFS.ifAny(listOf(this), { it.superTypes() }, { it.isFunction() })
fun IrType.isSuspendFunctionTypeOrSubtype(): Boolean = DFS.ifAny(listOf(this), { it.superTypes() }, { it.isSuspendFunction() })

fun IrType.isTypeParameter() = classifierOrNull is IrTypeParameterSymbol

fun IrType.isInterface() = classOrNull?.owner?.kind == ClassKind.INTERFACE

fun IrType.isFunctionOrKFunction() = isFunction() || isKFunction()

@Deprecated(
    "Use org.jetbrains.kotlin.ir.types.isNullable instead.",
    ReplaceWith("this.isNullable()", "org.jetbrains.kotlin.ir.types.isNullable")
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun IrType.isNullable(): Boolean = this.irTreeTypeUtils_isNullable()

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


fun IrType.substitute(params: List<IrTypeParameter>, arguments: List<IrType>): IrType =
    substitute(params.map { it.symbol }.zip(arguments).toMap())


fun IrType.substitute(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap[classifier]?.let { return it }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substitute(substitutionMap), it.variance)
        } else {
            it
        }
    }

    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }
    return IrSimpleTypeImpl(
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations
    )
}

private fun getImmediateSupertypes(irClass: IrClass): List<IrSimpleType> {
    val originalSupertypes = irClass.superTypes
    val args = irClass.defaultType.arguments.mapNotNull { (it as? IrTypeProjection)?.type }
    return originalSupertypes
        .filter { it.classOrNull != null }
        .map { superType ->
            superType.substitute(superType.classOrNull!!.owner.typeParameters, args) as IrSimpleType
        }
}

private fun collectAllSupertypes(irClass: IrClass, result: MutableSet<IrSimpleType>) {
    val immediateSupertypes = getImmediateSupertypes(irClass)
    result.addAll(immediateSupertypes)
    for (supertype in immediateSupertypes) {
        supertype.classOrNull.let { collectAllSupertypes(it!!.owner, result) }
    }
}

fun getAllSupertypes(irClass: IrClass): MutableSet<IrSimpleType> {
    val result = HashSet<IrSimpleType>()

    collectAllSupertypes(irClass, result)
    return result
}