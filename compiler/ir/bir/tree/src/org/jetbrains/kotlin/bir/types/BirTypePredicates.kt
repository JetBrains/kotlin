/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirPackageFragment
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.types.utils.classifierOrNull
import org.jetbrains.kotlin.bir.types.utils.isMarkedNullable
import org.jetbrains.kotlin.bir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

// The contents of irTypePredicates.kt is to be replaced by some de-duplicated code.


fun BirClassifierSymbol.isClassWithFqName(fqName: FqNameUnsafe): Boolean =
    this is BirClassSymbol && classFqNameEquals(this, fqName)

private fun classFqNameEquals(symbol: BirClassSymbol, fqName: FqNameUnsafe): Boolean {
    return classFqNameEquals(symbol as BirClass, fqName)
}

private fun classFqNameEquals(declaration: BirClass, fqName: FqNameUnsafe): Boolean =
    declaration.hasEqualFqName(fqName.toSafe())


fun BirType.isNotNullClassType(signature: IdSignature.CommonSignature) = isClassType(signature, nullable = false)
fun BirType.isNullableClassType(signature: IdSignature.CommonSignature) = isClassType(signature, nullable = true)

fun BirType.isClassType(signature: IdSignature.CommonSignature, nullable: Boolean? = null): Boolean {
    if (this !is BirSimpleType) return false
    if (nullable != null && this.isMarkedNullable() != nullable) return false
    return signature == classifier.signature ||
            classifier.let { it is BirClass && it.hasFqNameEqualToSignature(signature) }
}

private fun BirClass.hasFqNameEqualToSignature(signature: IdSignature.CommonSignature): Boolean =
    name.asString() == signature.shortName &&
            hasEqualFqName(FqName("${signature.packageFqName}.${signature.declarationFqName}"))

val kotlinPackageFqn = FqName.fromSegments(listOf("kotlin"))
private val kotlinReflectionPackageFqn = kotlinPackageFqn.child(Name.identifier("reflect"))
private val kotlinCoroutinesPackageFqn = kotlinPackageFqn.child(Name.identifier("coroutines"))

fun BirType.isFunctionMarker(): Boolean = classifierOrNull?.isClassWithName("Function", kotlinPackageFqn) == true
fun BirType.isFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("Function", kotlinPackageFqn) == true
fun BirType.isKFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("KFunction", kotlinReflectionPackageFqn) == true
fun BirType.isSuspendFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("SuspendFunction", kotlinCoroutinesPackageFqn) == true
fun BirType.isKSuspendFunction(): Boolean = classifierOrNull?.isClassWithNamePrefix("KSuspendFunction", kotlinReflectionPackageFqn) == true

private fun BirClassifierSymbol.isClassWithName(name: String, packageFqName: FqName): Boolean {
    return this is BirClass
            && name == this.name.asString()
            && (parent as? BirPackageFragment)?.packageFqName == packageFqName
}

private fun BirClassifierSymbol.isClassWithNamePrefix(prefix: String, packageFqName: FqName): Boolean {
    return this is BirClass
            && name.asString().startsWith(prefix)
            && (parent as? BirPackageFragment)?.packageFqName == packageFqName
}


// todo: maybe replace those with something more... direct?
fun BirType.isUnit() = isNotNullClassType(IdSignatureValues.unit)
fun BirType.isAny(): Boolean = isNotNullClassType(IdSignatureValues.any)
fun BirType.isNullableAny(): Boolean = isNullableClassType(IdSignatureValues.any)
fun BirType.isString(): Boolean = isNotNullClassType(IdSignatureValues.string)
fun BirType.isNullableString(): Boolean = isNullableClassType(IdSignatureValues.string)
fun BirType.isStringClassType(): Boolean = isClassType(IdSignatureValues.string)
fun BirType.isArray(): Boolean = isNotNullClassType(IdSignatureValues.array)
fun BirType.isNullableArray(): Boolean = isNullableClassType(IdSignatureValues.array)
fun BirType.isCollection(): Boolean = isNotNullClassType(IdSignatureValues.collection)
fun BirType.isNothing(): Boolean = isNotNullClassType(IdSignatureValues.nothing)
fun BirType.isNullableNothing(): Boolean = isNullableClassType(IdSignatureValues.nothing)
