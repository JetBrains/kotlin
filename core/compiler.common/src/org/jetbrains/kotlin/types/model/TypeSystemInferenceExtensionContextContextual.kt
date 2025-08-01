/*
* Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.types.model

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean = with(c) { contains(predicate) }

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: KotlinTypeMarker?): KotlinTypeMarker =
    with(c) { getApproximatedIntegerLiteralType(expectedType) }

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean = with(c) { isCapturedTypeConstructor() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.eraseContainingTypeParameters(): KotlinTypeMarker = with(c) { eraseContainingTypeParameters() }

context(c: TypeSystemInferenceExtensionContext)
fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? = with(c) { singleBestRepresentative() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isUnit(): Boolean = with(c) { isUnit() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean = with(c) { isBuiltinFunctionTypeOrSubtype() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker = with(c) { removeAnnotations() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker = with(c) { removeExactAnnotation() }

context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker = with(c) { replaceArguments(newArguments) }

context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker =
    with(c) { replaceArguments(replacement) }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker =
    with(c) { replaceArguments(replacement) }

context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker =
    with(c) { replaceArgumentsDeeply(replacement) }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker =
    with(c) { replaceArgumentsDeeply(replacement) }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.hasExactAnnotation(): Boolean = with(c) { hasExactAnnotation() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean = with(c) { hasNoInferAnnotation() }

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isFinalClassConstructor(): Boolean = with(c) { isFinalClassConstructor() }

context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableMarker.freshTypeConstructor(): TypeVariableTypeConstructorMarker = with(c) { freshTypeConstructor() }

context(c: TypeSystemInferenceExtensionContext)
fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker = with(c) { typeConstructorProjection() }

context(c: TypeSystemInferenceExtensionContext)
fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? = with(c) { typeParameter() }

context(c: TypeSystemInferenceExtensionContext)
fun CapturedTypeMarker.hasRawSuperTypeRecursive(): Boolean = with(c) { hasRawSuperTypeRecursive() }

context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableMarker.defaultType(): SimpleTypeMarker = with(c) { defaultType() }

context(c: TypeSystemInferenceExtensionContext)
fun RigidTypeMarker.getUpperBoundForApproximationOfIntersectionType(): KotlinTypeMarker? =
    with(c) { getUpperBoundForApproximationOfIntersectionType() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isSpecial(): Boolean = with(c) { isSpecial() }

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isTypeVariable(): Boolean = with(c) { isTypeVariable() }

context(c: TypeSystemInferenceExtensionContext)
fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean =
    with(c) { isContainedInInvariantOrContravariantPositions() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean = with(c) { isSignedOrUnsignedNumberType() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean = with(c) { isFunctionOrKFunctionWithAnySuspendability() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.functionTypeKind(): FunctionTypeKind? = with(c) { functionTypeKind() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.isExtensionFunctionType(): Boolean = with(c) { isExtensionFunctionType() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.contextParameterCount(): Int = with(c) { contextParameterCount() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<KotlinTypeMarker> =
    with(c) { extractArgumentsForFunctionTypeOrSubtype() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.getFunctionTypeFromSupertypes(): KotlinTypeMarker = with(c) { getFunctionTypeFromSupertypes() }

context(c: TypeSystemInferenceExtensionContext)
fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker = with(c) { getOriginalTypeVariable() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> = with(c) { extractTypeVariables() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> = with(c) { extractTypeParameters() }

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.convertToNonRaw(): KotlinTypeMarker = with(c) { convertToNonRaw() }
