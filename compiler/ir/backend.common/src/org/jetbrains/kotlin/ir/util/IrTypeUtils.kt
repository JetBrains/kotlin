/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
private val kotlinReflectionPackageFqn = kotlinPackageFqn.child(Name.identifier("reflect"))
private val kotlinCoroutinesPackageFqn = kotlinPackageFqn.child(Name.identifier("coroutines"))

fun IrType.isFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("Function", kotlinPackageFqn) == true
fun IrType.isKFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("KFunction", kotlinReflectionPackageFqn) == true
fun IrType.isSuspendFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("SuspendFunction", kotlinCoroutinesPackageFqn) == true
fun IrType.isKSuspendFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("KSuspendFunction", kotlinReflectionPackageFqn) == true

fun IrClassifierSymbol.isFunction(): Boolean = this.isClassWithNamePrefix("Function", kotlinPackageFqn)
fun IrClassifierSymbol.isKFunction(): Boolean = this.isClassWithNamePrefix("KFunction", kotlinReflectionPackageFqn)
fun IrClassifierSymbol.isKSuspendFunction(): Boolean = this.isClassWithNamePrefix("KSuspendFunction", kotlinReflectionPackageFqn)

private fun IrClassifierSymbol.isClassWithNamePrefix(prefix: String, packageFqName: FqName): Boolean {
    val declaration = owner as IrDeclarationWithName
    val name = declaration.name.asString()
    if (!name.startsWith(prefix)) return false
    val parent = declaration.parent as? IrPackageFragment ?: return false

    return parent.fqName == packageFqName
}

fun IrType.superTypes(): List<IrType> = classifierOrNull?.superTypes() ?: emptyList()

fun IrType.isFunctionTypeOrSubtype(): Boolean = DFS.ifAny(listOf(this), IrType::superTypes, IrType::isFunction)
fun IrType.isSuspendFunctionTypeOrSubtype(): Boolean = DFS.ifAny(listOf(this), IrType::superTypes, IrType::isSuspendFunction)

fun IrType.isTypeParameter() = classifierOrNull is IrTypeParameterSymbol

fun IrType.isInterface() = classOrNull?.owner?.kind == ClassKind.INTERFACE

fun IrType.isFunctionOrKFunction() = isFunction() || isKFunction()

fun IrType.isSuspendFunctionOrKFunction() = isSuspendFunction() || isKSuspendFunction()

fun IrType.isThrowable(): Boolean = isTypeFromKotlinPackage { name -> name.asString() == "Throwable" }

fun IrType.isUnsigned(): Boolean = isTypeFromKotlinPackage { name -> UnsignedTypes.isShortNameOfUnsignedType(name) }

private inline fun IrType.isTypeFromKotlinPackage(namePredicate: (Name) -> Boolean): Boolean {
    if (this is IrSimpleType) {
        val classClassifier = classifier as? IrClassSymbol ?: return false
        if (!namePredicate(classClassifier.owner.name)) return false
        val parent = classClassifier.owner.parent as? IrPackageFragment ?: return false
        return parent.fqName == kotlinPackageFqn
    } else return false
}

fun IrType.isPrimitiveArray() = isTypeFromKotlinPackage { it in FqNames.primitiveArrayTypeShortNames }

fun IrType.getPrimitiveArrayElementType() = (this as? IrSimpleType)?.let {
    (it.classifier.owner as? IrClass)?.fqNameWhenAvailable?.toUnsafe()?.let { fqn -> FqNames.arrayClassFqNameToPrimitiveType[fqn] }
}

fun IrType.substitute(params: List<IrTypeParameter>, arguments: List<IrType>): IrType =
    substitute(params.map { it.symbol }.zip(arguments).toMap())

fun IrType.substitute(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType || substitutionMap.isEmpty()) return this

    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }

    substitutionMap[classifier]?.let { substitutedType ->
        // Add nullability and annotations from original type
        return substitutedType
            .withHasQuestionMark(
                hasQuestionMark ||
                        substitutedType is IrSimpleType && substitutedType.hasQuestionMark
            )
            .addAnnotations(newAnnotations)
    }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substitute(substitutionMap), it.variance)
        } else {
            it
        }
    }

    return IrSimpleTypeImpl(
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations
    )
}

private fun getImmediateSupertypes(irType: IrSimpleType): List<IrSimpleType> {
    val irClass = irType.getClass()
        ?: throw AssertionError("Not a class type: ${irType.render()}")
    val originalSupertypes = irClass.superTypes
    val arguments =
        irType.arguments.map {
            it.typeOrNull
                ?: throw AssertionError("*-projection in supertype arguments: ${irType.render()}")
        }
    return originalSupertypes
        .filter { it.classOrNull != null }
        .map { superType ->
            superType.substitute(irClass.typeParameters, arguments) as IrSimpleType
        }
}

private fun collectAllSupertypes(irType: IrSimpleType, result: MutableSet<IrSimpleType>) {
    val immediateSupertypes = getImmediateSupertypes(irType)
    result.addAll(immediateSupertypes)
    for (supertype in immediateSupertypes) {
        collectAllSupertypes(supertype, result)
    }
}

// Given the following classes:
//      open class A<X>
//      open class B<Y> : A<List<Y>>
//      class C<Z> : B<List<Z>>
// for the class C, this function constructs:
//      { B<List<Z>>, A<List<List<Z>>, Any }
// where Z is a type parameter of class C.
fun getAllSubstitutedSupertypes(irClass: IrClass): MutableSet<IrSimpleType> {
    val result = HashSet<IrSimpleType>()
    collectAllSupertypes(irClass.defaultType, result)
    return result
}
