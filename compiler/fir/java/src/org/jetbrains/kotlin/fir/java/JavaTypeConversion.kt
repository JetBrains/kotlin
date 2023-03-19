/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
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
    DEFAULT, ANNOTATION_MEMBER, SUPERTYPE,
    TYPE_PARAMETER_BOUND_FIRST_ROUND, TYPE_PARAMETER_BOUND_AFTER_FIRST_ROUND
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
        ?: ConeErrorType(ConeSimpleDiagnostic("Type reference in Java not resolved: ${this::class.java}", DiagnosticKind.Java))

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
    mode: FirJavaTypeConversionMode,
    additionalAnnotations: Collection<JavaAnnotation>? = null
): ConeKotlinType =
    toConeTypeProjection(session, javaTypeParameterStack, Variance.INVARIANT, mode, additionalAnnotations).type
        ?: StandardClassIds.Any.toConeFlexibleType(emptyArray(), emptyArray(), ConeAttributes.Empty)

private fun JavaType?.toConeTypeProjection(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack,
    parameterVariance: Variance, mode: FirJavaTypeConversionMode,
    additionalAnnotations: Collection<JavaAnnotation>? = null
): ConeTypeProjection {
    val attributes = if (this != null && (annotations.isNotEmpty() || additionalAnnotations != null)) {
        val convertedAnnotations = buildList {
            if (annotations.isNotEmpty()) {
                addAll(this@toConeTypeProjection.convertAnnotationsToFir(session, javaTypeParameterStack))
            }
            if (additionalAnnotations != null) {
                addAll(additionalAnnotations.convertAnnotationsToFir(session, javaTypeParameterStack))
            }
        }

        ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(convertedAnnotations)))
    } else {
        ConeAttributes.Empty
    }

    return when (this) {
        is JavaClassifierType -> {
            val lowerBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes)
            if (mode == FirJavaTypeConversionMode.ANNOTATION_MEMBER) {
                return lowerBound
            }
            val upperBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, mode, attributes, lowerBound)

            val finalLowerBound = when (lowerBound) {
                is ConeTypeParameterType ->
                    ConeDefinitelyNotNullType.create(
                        lowerBound, session.typeContext,
                        // Upper bounds might be not initialized properly yet, so we force creating DefinitelyNotNullType
                        // It should not affect semantics, since it would be still a valid type anyway
                        avoidComprehensiveCheck = true,
                    ) ?: lowerBound

                else -> lowerBound
            }

            if (isRaw) ConeRawType.create(finalLowerBound, upperBound) else ConeFlexibleType(finalLowerBound, upperBound)
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
                val nullabilityAnnotationOnWildcard = extractNullabilityAnnotationOnBoundedWildcard(this)?.let(::listOf)
                val boundType = bound.toConeKotlinType(session, javaTypeParameterStack, mode, nullabilityAnnotationOnWildcard)
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

            val lookupTag = classId.toLookupTag()
            // When converting type parameter bounds we should not attempt to load any classes, as this may trigger
            // enhancement of type parameter bounds on some other class that depends on this one. Also, in case of raw
            // types specifically there could be an infinite recursion on the type parameter itself.
            val mappedTypeArguments = when {
                isRaw -> {
                    val typeParameterSymbols =
                        lookupTag.takeIf { lowerBound == null && mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }
                            ?.toFirRegularClassSymbol(session)?.typeParameterSymbols
                    // Given `C<T : X>`, `C` -> `C<X>..C<*>?`.
                    when (mode) {
                        FirJavaTypeConversionMode.ANNOTATION_MEMBER -> Array(classifier.typeParameters.size) { ConeStarProjection }
                        else -> typeParameterSymbols?.getProjectionsForRawType(session)
                            ?: Array(classifier.typeParameters.size) { ConeStarProjection }
                    }
                }

                lookupTag != lowerBound?.lookupTag && typeArguments.isNotEmpty() -> {
                    val typeParameterSymbols =
                        lookupTag.takeIf { mode != FirJavaTypeConversionMode.TYPE_PARAMETER_BOUND_FIRST_ROUND }
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

        else -> ConeErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
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
