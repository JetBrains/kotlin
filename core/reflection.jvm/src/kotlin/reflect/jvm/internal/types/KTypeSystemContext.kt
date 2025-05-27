/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.*
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KClassImpl
import kotlin.reflect.jvm.internal.KTypeParameterImpl

object KTypeSystemContext : TypeSystemContext {
    override fun KotlinTypeMarker.asRigidType(): RigidTypeMarker? {
        return if (asFlexibleType() != null) null else this as RigidTypeMarker
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        return if (this is AbstractKType && lowerBoundIfFlexible() != null) this else null
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.isUninferredParameter(): Boolean {
        TODO("Not yet implemented")
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun FlexibleTypeMarker.upperBound(): RigidTypeMarker {
        return (this as AbstractKType).upperBoundIfFlexible() as AbstractKType
    }

    override fun FlexibleTypeMarker.lowerBound(): RigidTypeMarker {
        return (this as AbstractKType).lowerBoundIfFlexible() as AbstractKType
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        return this as? CapturedTypeMarker
    }

    override fun RigidTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        return if (this is AbstractKType && isDefinitelyNotNullType) this else null
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(preserveAttributes: Boolean): KotlinTypeMarker {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.makeDefinitelyNotNullOrNotNull(): RigidTypeMarker {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.isMarkedNullable(): Boolean {
        return (this as KType).isMarkedNullable
    }

    override fun RigidTypeMarker.withNullability(nullable: Boolean): RigidTypeMarker {
        if (this is CapturedKType)
            return if (nullable == isMarkedNullable) this else CapturedKType(lowerType, typeConstructor, nullable)
        this as AbstractKType
        return makeNullableAsSpecified(nullable)
    }

    override fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker {
        if (this is CapturedKType) return typeConstructor
        this as AbstractKType
        if (isNothingType) return NothingKClass
        (classifier as? KClassImpl<*>)?.let { kClass ->
            if (kClass.java.componentType?.isPrimitive == false) {
                // Non-primitive arrays of different element types are represented by different Class (and thus KClass) objects. However,
                // they should have the same type constructor `kotlin.Array`, so that subtype checking would work correctly.
                // Note that another possible way to fix this would be to tweak `areEqualTypeConstructors`.
                return Array::class as TypeConstructorMarker
            }
        }
        return (mutableCollectionClass ?: classifier) as TypeConstructorMarker
    }

    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        TODO("Not yet implemented")
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean {
        return false
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        return (this as CapturedKType).typeConstructor
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        return CaptureStatus.FOR_SUBTYPING
    }

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean {
        return false
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        return KTypeProjectionImpl((this as CapturedKTypeConstructor).projection)
    }

    override fun KotlinTypeMarker.argumentsCount(): Int {
        return (this as KType).arguments.size
    }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        return KTypeProjectionImpl((this as KType).arguments[index])
    }

    override fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker> {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.isStubType(): Boolean {
        return false
    }

    override fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker {
        TODO("Not yet implemented")
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
        return (this as CapturedKType).lowerType as KotlinTypeMarker?
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        return (this as KTypeProjectionImpl).value == KTypeProjection.STAR
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        return (this as KTypeProjectionImpl).value.variance?.convertVariance() ?: TypeVariance.OUT
    }

    private fun KVariance.convertVariance(): TypeVariance = when (this) {
        KVariance.INVARIANT -> TypeVariance.INV
        KVariance.IN -> TypeVariance.IN
        KVariance.OUT -> TypeVariance.OUT
    }

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker? {
        return (this as KTypeProjectionImpl).value.type as KotlinTypeMarker?
    }

    override fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.parametersCount(): Int {
        return if (this is KClass<*>) allTypeParameters().size else 0
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        return (this as KClass<*>).allTypeParameters()[index] as KTypeParameterImpl
    }

    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return when (this) {
            is KClass<*> -> supertypes.map { it as KotlinTypeMarker }
            is KTypeParameter -> upperBounds.map { it as KotlinTypeMarker }
            is CapturedKTypeConstructor -> supertypes.map { it as KotlinTypeMarker }
            else -> error("Unsupported type constructor: $this (${this::class.java.name})")
        }
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        return this is KClass<*>
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() = TODO("Not yet implemented")

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        return (this as KTypeParameter).variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        TODO("Not yet implemented")
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker {
        TODO("Not yet implemented")
    }

    override fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker> {
        TODO("Not yet implemented")
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        TODO("Not yet implemented")
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        return c1 == c2
    }

    override fun TypeConstructorMarker.isDenotable(): Boolean {
        return this !is CapturedKTypeConstructor
    }

    override fun KotlinTypeMarker.isNullableType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker> {
        TODO("Not yet implemented")
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        return this is KClassImpl<*> && isFinal &&
                classKind != ClassKind.ENUM_CLASS && classKind != ClassKind.ENUM_ENTRY && classKind != ClassKind.ANNOTATION_CLASS
    }

    override fun captureFromArguments(type: RigidTypeMarker, status: CaptureStatus): RigidTypeMarker? {
        return captureKTypeFromArguments(type as KType) as AbstractKType?
    }

    override fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker? {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker {
        return this as TypeArgumentListMarker
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        return this == Any::class
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        return this == Nothing::class
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        TODO("Not yet implemented")
    }

    override fun RigidTypeMarker.isSingleClassifierType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun intersectTypes(types: Collection<KotlinTypeMarker>): KotlinTypeMarker {
        TODO("Not yet implemented")
    }

    override fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker {
        TODO("Not yet implemented")
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker> {
        TODO("Not yet implemented")
    }

    override fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy {
        val substitutor = KTypeSubstitutor.create(type as KType)
        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: KotlinTypeMarker): RigidTypeMarker {
                return substitutor.substitute(type.lowerBoundIfFlexible() as KType).type as AbstractKType
            }
        }
    }

    override fun KotlinTypeMarker.isTypeVariableType(): Boolean {
        TODO("Not yet implemented")
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker {
        TODO("Not yet implemented")
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        TODO("Not yet implemented")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker {
        TODO("Not yet implemented")
    }

    override fun KotlinTypeMarker.isDynamic(): Boolean =
        false
}
