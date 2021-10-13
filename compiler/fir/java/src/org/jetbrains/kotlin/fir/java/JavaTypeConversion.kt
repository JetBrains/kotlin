/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

private fun ClassId.toLookupTag(): ConeClassLikeLookupTag =
    ConeClassLikeLookupTagImpl(this)

private fun ClassId.toConeFlexibleType(
    typeArguments: Array<out ConeTypeProjection>,
    typeArgumentsForUpper: Array<out ConeTypeProjection>,
    attributes: ConeAttributes
) = toLookupTag().run {
    ConeFlexibleType(
        constructClassType(typeArguments, isNullable = false, attributes),
        constructClassType(typeArgumentsForUpper, isNullable = true, attributes)
    )
}

internal enum class FirJavaTypeConversionMode {
    DEFAULT, ANNOTATION_MEMBER, SUPERTYPE, TYPE_PARAMETER_BOUND
}

internal fun FirTypeRef.resolveIfJavaType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): FirTypeRef = when (this) {
    is FirResolvedTypeRef -> this
    is FirJavaTypeRef -> type.toFirResolvedTypeRef(session, javaTypeParameterStack, mode)
    else -> this
}

internal fun FirTypeRef.toConeKotlinTypeProbablyFlexible(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): ConeKotlinType =
    (resolveIfJavaType(session, javaTypeParameterStack, mode) as? FirResolvedTypeRef)?.type
        ?: ConeKotlinErrorType(ConeSimpleDiagnostic("Type reference in Java not resolved: ${this::class.java}", DiagnosticKind.Java))

internal fun JavaType.toFirJavaTypeRef(session: FirSession, javaTypeParameterStack: JavaTypeParameterStack): FirJavaTypeRef {
    return buildJavaTypeRef {
        annotationBuilder = { convertAnnotationsToFir(session, javaTypeParameterStack) }
        type = this@toFirJavaTypeRef
    }
}

internal fun JavaType?.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): FirResolvedTypeRef {
    return buildResolvedTypeRef {
        type = toConeKotlinType(session, javaTypeParameterStack, mode)
            .let { if (mode == FirJavaTypeConversionMode.SUPERTYPE) it.lowerBoundIfFlexible() else it }
        annotations += type.attributes.customAnnotations
    }
}

private fun JavaType?.toConeKotlinType(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode
): ConeKotlinType =
    toConeTypeProjection(session, javaTypeParameterStack, Variance.INVARIANT, mode).type
        ?: StandardClassIds.Any.toConeFlexibleType(emptyArray(), emptyArray(), ConeAttributes.Empty)

private fun JavaType?.toConeTypeProjection(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    parameterVariance: Variance, mode: FirJavaTypeConversionMode
): ConeTypeProjection {
    val attributes = if (this != null && annotations.isNotEmpty())
        ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(convertAnnotationsToFir(session, javaTypeParameterStack))))
    else
        ConeAttributes.Empty

    return when (this) {
        is JavaClassifierType -> {
            val lowerBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes)
            if (mode == FirJavaTypeConversionMode.ANNOTATION_MEMBER) {
                return lowerBound // TODO: `KClass<Any>` is wrong for raw `Class`
            }
            val upperBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes, lowerBound)
            if (isRaw) ConeRawType(lowerBound, upperBound) else ConeFlexibleType(lowerBound, upperBound)
        }

        is JavaArrayType -> {
            val (classId, arguments) = when (val componentType = componentType) {
                is JavaPrimitiveType ->
                    StandardClassIds.byName(componentType.type!!.arrayTypeName.identifier) to arrayOf()
                else ->
                    StandardClassIds.Array to arrayOf(componentType.toConeKotlinType(session, javaTypeParameterStack, mode))
            }
            if (mode == FirJavaTypeConversionMode.ANNOTATION_MEMBER) {
                classId.constructClassLikeType(arguments, isNullable = false, attributes)
            } else {
                val argumentsForUpper = Array(arguments.size) { ConeKotlinTypeProjectionOut(arguments[it]) }
                classId.toConeFlexibleType(arguments, argumentsForUpper, attributes)
            }
        }

        is JavaPrimitiveType ->
            StandardClassIds.byName(type?.typeName?.identifier ?: "Unit")
                .constructClassLikeType(emptyArray(), isNullable = false, attributes)

        is JavaWildcardType -> {
            // TODO: this discards annotations on wildcards, allowed since Java 8 - what do they mean?
            //    List<@NotNull ? extends @Nullable Object>
            val bound = this.bound
            val argumentVariance = if (isExtends) Variance.OUT_VARIANCE else Variance.IN_VARIANCE
            if (bound == null || (parameterVariance != Variance.INVARIANT && parameterVariance != argumentVariance)) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinType(session, javaTypeParameterStack, mode)
                if (isExtends) ConeKotlinTypeProjectionOut(boundType) else ConeKotlinTypeProjectionIn(boundType)
            }
        }

        null -> ConeStarProjection
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode,
    attributes: ConeAttributes,
    lowerBound: ConeLookupTagBasedType? = null
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            var classId = if (mode == FirJavaTypeConversionMode.ANNOTATION_MEMBER) {
                JavaToKotlinClassMap.mapJavaToKotlinIncludingClassMapping(classifier.fqName!!)
            } else {
                JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!)
            } ?: classifier.classId!!

            if (lowerBound == null || argumentsMakeSenseOnlyForMutableContainer(classId, session)) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            // When converting type parameter bounds we should not attempt to load any classes, as this may trigger
            // enhancement of type parameter bounds on some other class that depends on this one. Also, in case of raw
            // types specifically there could be an infinite recursion on the type parameter itself.
            val mappedTypeArguments = when {
                isRaw -> {
                    val typeParameterSymbols =
                        lookupTag.takeIf { lowerBound == null && mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND }
                            ?.toFirRegularClassSymbol(session)?.typeParameterSymbols
                    // Given `C<T : X>`, `C` -> `C<X>..C<*>?`.
                    typeParameterSymbols?.eraseToUpperBounds(session)
                        ?: Array(classifier.typeParameters.size) { ConeStarProjection }
                }
                lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> {
                    val typeParameterSymbols =
                        lookupTag.takeIf { mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND }
                            ?.toFirRegularClassSymbol(session)?.typeParameterSymbols
                    Array(typeArguments.size) { index ->
                        // TODO: check this
                        val newMode = if (mode == FirJavaTypeConversionMode.ANNOTATION_MEMBER) FirJavaTypeConversionMode.DEFAULT else mode
                        val argument = typeArguments[index]
                        val variance = typeParameterSymbols?.getOrNull(index)?.fir?.variance ?: Variance.INVARIANT
                        argument.toConeTypeProjection(session, javaTypeParameterStack, variance, newMode)
                    }
                }
                else -> lowerBound?.typeArguments
            }

            lookupTag.constructClassType(mappedTypeArguments ?: emptyArray(), isNullable = lowerBound != null, attributes)
        }
        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            ConeTypeParameterTypeImpl(symbol.toLookupTag(), isNullable = lowerBound != null, attributes)
        }
        null -> {
            val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
            classId.constructClassLikeType(emptyArray(), isNullable = lowerBound != null, attributes)
        }
        else -> ConeKotlinErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
    }
}

// Returns true for covariant read-only container that has mutable pair with invariant parameter
// List<in A> does not make sense, but MutableList<in A> does
// Same for Map<K, in V>
// But both Iterable<in A>, MutableIterable<in A> don't make sense as they are covariant, so return false
private fun JavaClassifierType.argumentsMakeSenseOnlyForMutableContainer(
    classId: ClassId,
    session: FirSession,
): Boolean {
    if (!JavaToKotlinClassMap.isReadOnly(classId.asSingleFqName().toUnsafe())) return false
    val mutableClassId = classId.readOnlyToMutable() ?: return false

    if (!typeArguments.lastOrNull().isSuperWildcard()) return false
    val mutableLastParameterVariance =
        mutableClassId.toLookupTag().toFirRegularClassSymbol(session)?.typeParameterSymbols?.lastOrNull()?.variance
            ?: return false

    return mutableLastParameterVariance != Variance.OUT_VARIANCE
}

private fun List<FirTypeParameterSymbol>.eraseToUpperBounds(session: FirSession): Array<ConeTypeProjection> {
    val cache = mutableMapOf<FirTypeParameter, ConeKotlinType>()
    return Array(size) { index -> this[index].fir.eraseToUpperBound(session, cache) }
}

private fun FirTypeParameter.eraseToUpperBound(session: FirSession, cache: MutableMap<FirTypeParameter, ConeKotlinType>): ConeKotlinType {
    return cache.getOrPut(this) {
        // Mark to avoid loops.
        cache[this] = ConeKotlinErrorType(ConeIntermediateDiagnostic("self-recursive type parameter $name"))
        // We can assume that Java type parameter bounds are already converted.
        bounds.first().coneType.eraseAsUpperBound(session, cache)
    }
}

private fun ConeKotlinType.eraseAsUpperBound(session: FirSession, cache: MutableMap<FirTypeParameter, ConeKotlinType>): ConeKotlinType =
    when (this) {
        is ConeClassLikeType ->
            withArguments(typeArguments.map { ConeStarProjection }.toTypedArray())
        is ConeFlexibleType ->
            // If one bound is a type parameter, the other is probably the same type parameter,
            // so there is no exponential complexity here due to cache lookups.
            coneFlexibleOrSimpleType(
                session.typeContext,
                lowerBound.eraseAsUpperBound(session, cache),
                upperBound.eraseAsUpperBound(session, cache)
            )
        is ConeTypeParameterType ->
            lookupTag.typeParameterSymbol.fir.eraseToUpperBound(session, cache).let {
                if (isNullable) it.withNullability(nullability, session.typeContext) else it
            }
        is ConeDefinitelyNotNullType ->
            original.eraseAsUpperBound(session, cache).makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext)
        else -> error("unexpected Java type parameter upper bound kind: $this")
    }
