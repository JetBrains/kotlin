/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.typeEnhancement.TypeComponentPosition
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private fun ClassId.toLookupTag(): ConeClassLikeLookupTag {
    return ConeClassLikeLookupTagImpl(this)
}

internal fun ClassId.toConeKotlinType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return ConeClassLikeTypeImpl(toLookupTag(), typeArguments, isNullable, attributes)
}

internal fun FirTypeRef.toConeKotlinTypeProbablyFlexible(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType =
    when (this) {
        is FirResolvedTypeRef -> type
        is FirJavaTypeRef -> type.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = false)
        else -> ConeKotlinErrorType(
            ConeSimpleDiagnostic("Unexpected type reference in JavaClassUseSiteMemberScope: ${this::class.java}", DiagnosticKind.Java)
        )
    }

internal fun JavaType.toFirJavaTypeRef(session: FirSession, javaTypeParameterStack: JavaTypeParameterStack): FirJavaTypeRef {
    return buildJavaTypeRef {
        annotationBuilder = {
            (this@toFirJavaTypeRef as? JavaClassifierType)?.annotations.orEmpty().map {
                it.toFirAnnotationCall(session, javaTypeParameterStack)
            }
        }
        type = this@toFirJavaTypeRef
    }
}

internal fun JavaClassifierType.toFirResolvedTypeRef(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    isForSupertypes: Boolean,
    forTypeParameterBounds: Boolean
): FirResolvedTypeRef {
    val coneType =
        if (isForSupertypes)
            toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, forTypeParameterBounds, isForSupertypes)
        else
            toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, forTypeParameterBounds, isForSupertypes)

    return buildResolvedTypeRef {
        type = coneType
        this@toFirResolvedTypeRef.annotations.mapTo(annotations) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
    }
}

internal fun JavaType?.toConeKotlinTypeWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    forAnnotationMember: Boolean = false,
    isForSupertypes: Boolean = false
): ConeKotlinType {
    val attributes = if (this != null && annotations.isNotEmpty()) {
        ConeAttributes.create(
            listOf(CustomAnnotationTypeAttribute(annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) }))
        )
    } else {
        ConeAttributes.Empty
    }
    return when (this) {
        is JavaClassifierType -> {
            toConeKotlinTypeWithoutEnhancement(
                session,
                javaTypeParameterStack,
                forAnnotationMember = forAnnotationMember,
                attributes = attributes
            )
        }
        is JavaPrimitiveType -> {
            val primitiveType = type
            val kotlinPrimitiveName = when (val javaName = primitiveType?.typeName?.asString()) {
                null -> "Unit"
                else -> javaName.capitalizeAsciiOnly()
            }

            val classId = StandardClassIds.byName(kotlinPrimitiveName)
            classId.toConeKotlinType(emptyArray(), isNullable = false, attributes)
        }
        is JavaArrayType -> {
            toConeKotlinTypeWithoutEnhancement(
                session,
                javaTypeParameterStack,
                forAnnotationMember,
                isForSupertypes,
                attributes = attributes
            )
        }
        is JavaWildcardType ->
            bound?.toConeKotlinTypeWithoutEnhancement(
                session,
                javaTypeParameterStack,
                isForSupertypes = isForSupertypes
            ) ?: StandardClassIds.Any.toConeFlexibleType(emptyArray(), attributes = attributes)
        null ->
            StandardClassIds.Any.toConeFlexibleType(emptyArray(), attributes = attributes)
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

private fun JavaArrayType.toConeKotlinTypeWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    forAnnotationValueParameter: Boolean = false,
    isForSupertypes: Boolean,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeKotlinType {
    val componentType = componentType
    return if (componentType !is JavaPrimitiveType) {
        val classId = StandardClassIds.Array
        val argumentType = componentType.toConeKotlinTypeWithoutEnhancement(
            session, javaTypeParameterStack, forAnnotationValueParameter, isForSupertypes
        )
        if (forAnnotationValueParameter) {
            classId.constructClassLikeType(arrayOf(argumentType), isNullable = false, attributes = attributes)
        } else {
            classId.toConeFlexibleType(
                arrayOf(argumentType),
                typeArgumentsForUpper = arrayOf(ConeKotlinTypeProjectionOut(argumentType)),
                attributes = attributes
            )
        }
    } else {
        val javaComponentName = componentType.type?.typeName?.asString()?.capitalizeAsciiOnly() ?: error("Array of voids")
        val classId = StandardClassIds.byName(javaComponentName + "Array")

        if (forAnnotationValueParameter) {
            classId.constructClassLikeType(emptyArray(), isNullable = false, attributes = attributes)
        } else {
            classId.toConeFlexibleType(emptyArray(), attributes = attributes)
        }
    }
}

private fun ClassId.toConeFlexibleType(
    typeArguments: Array<ConeTypeProjection>,
    typeArgumentsForUpper: Array<ConeTypeProjection> = typeArguments,
    attributes: ConeAttributes = ConeAttributes.Empty
) = ConeFlexibleType(
    toConeKotlinType(typeArguments, isNullable = false, attributes),
    toConeKotlinType(typeArgumentsForUpper, isNullable = true, attributes)
)

private fun JavaClassifierType.toConeKotlinTypeWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    forTypeParameterBounds: Boolean = false,
    isForSupertypes: Boolean = false,
    forAnnotationMember: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeKotlinType {
    val lowerBound = toConeKotlinTypeForFlexibleBound(
        session,
        javaTypeParameterStack,
        forTypeParameterBounds,
        isForSupertypes,
        forAnnotationMember = forAnnotationMember,
        attributes = attributes
    )
    if (forAnnotationMember) {
        return lowerBound
    }
    val upperBound =
        toConeKotlinTypeForFlexibleBound(
            session,
            javaTypeParameterStack,
            forTypeParameterBounds,
            isForSupertypes,
            lowerBound,
            forAnnotationMember = forAnnotationMember,
            attributes = attributes
        )

    return if (isRaw)
        ConeRawType(lowerBound, upperBound)
    else
        ConeFlexibleType(lowerBound, upperBound)
}

private fun computeRawProjection(
    session: FirSession,
    parameter: FirTypeParameter,
    attr: TypeComponentPosition,
    erasedUpperBound: ConeKotlinType = parameter.getErasedUpperBound(session)
) = when (attr) {
    // Raw(List<T>) => (List<Any?>..List<*>)
    // Raw(Enum<T>) => (Enum<Enum<*>>..Enum<out Enum<*>>)
    // In the last case upper bound is equal to star projection `Enum<*>`,
    // but we want to keep matching tree structure of flexible bounds (at least they should have the same size)
    TypeComponentPosition.FLEXIBLE_LOWER -> {
        // T : String -> String
        // in T : String -> String
        // T : Enum<T> -> Enum<*>
        erasedUpperBound
    }
    TypeComponentPosition.FLEXIBLE_UPPER, TypeComponentPosition.INFLEXIBLE -> {
        if (!parameter.variance.allowsOutPosition)
        // in T -> Comparable<Nothing>
            session.builtinTypes.nothingType.type
        else if (erasedUpperBound is ConeClassLikeType &&
            erasedUpperBound.lookupTag.toSymbol(session)!!.firUnsafe<FirRegularClass>().typeParameters.isNotEmpty()
        )
        // T : Enum<E> -> out Enum<*>
            ConeKotlinTypeProjectionOut(erasedUpperBound)
        else
        // T : String -> *
            ConeStarProjection
    }
}

// Definition:
// ErasedUpperBound(T : G<t>) = G<*> // UpperBound(T) is a type G<t> with arguments
// ErasedUpperBound(T : A) = A // UpperBound(T) is a type A without arguments
// ErasedUpperBound(T : F) = UpperBound(F) // UB(T) is another type parameter F
private fun FirTypeParameter.getErasedUpperBound(
    session: FirSession,
    // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
    // E.g. `class A<T extends A, F extends A>`
    // To prevent recursive calls return defaultValue() instead
    potentiallyRecursiveTypeParameter: FirTypeParameter? = null,
    defaultValue: (() -> ConeKotlinType) = {
        ConeKotlinErrorType(ConeIntermediateDiagnostic("Can't compute erased upper bound of type parameter `$this`"))
    }
): ConeKotlinType {
    if (this === potentiallyRecursiveTypeParameter) return defaultValue()

    val firstUpperBound = this.bounds.first().coneType

    return getErasedVersionOfFirstUpperBound(session, firstUpperBound, mutableSetOf(this, potentiallyRecursiveTypeParameter), defaultValue)
}

private fun getErasedVersionOfFirstUpperBound(
    session: FirSession,
    firstUpperBound: ConeKotlinType,
    alreadyVisitedParameters: MutableSet<FirTypeParameter?>,
    defaultValue: () -> ConeKotlinType
): ConeKotlinType =
    when (firstUpperBound) {
        is ConeClassLikeType ->
            firstUpperBound.withArguments(firstUpperBound.typeArguments.map { ConeStarProjection }.toTypedArray())

        is ConeFlexibleType -> {
            val lowerBound =
                getErasedVersionOfFirstUpperBound(session, firstUpperBound.lowerBound, alreadyVisitedParameters, defaultValue)
                    .lowerBoundIfFlexible()
            if (firstUpperBound.upperBound is ConeTypeParameterType) {
                // Avoid exponential complexity
                ConeFlexibleType(
                    lowerBound,
                    lowerBound.withNullability(ConeNullability.NULLABLE, session.inferenceComponents.ctx)
                )
            } else {
                ConeFlexibleType(
                    lowerBound,
                    getErasedVersionOfFirstUpperBound(session, firstUpperBound.upperBound, alreadyVisitedParameters, defaultValue)
                )
            }
        }
        is ConeTypeParameterType -> {
            val current = firstUpperBound.lookupTag.typeParameterSymbol.fir

            if (alreadyVisitedParameters.add(current)) {
                val nextUpperBound = current.bounds.first().coneType
                getErasedVersionOfFirstUpperBound(session, nextUpperBound, alreadyVisitedParameters, defaultValue)
            } else {
                defaultValue()
            }
        }
        else -> error("Unexpected kind of firstUpperBound: $firstUpperBound [${firstUpperBound::class}]")
    }

private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    forTypeParameterBounds: Boolean,
    isForSupertypes: Boolean,
    lowerBound: ConeLookupTagBasedType? = null,
    forAnnotationMember: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            //val classId = classifier.classId!!
            var classId = if (forAnnotationMember) {
                JavaToKotlinClassMap.mapJavaToKotlinIncludingClassMapping(classifier.fqName!!)
            } else {
                JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!)
            } ?: classifier.classId!!

            if (lowerBound == null || argumentsMakeSenseOnlyForMutableContainer(classId, session)) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            if (lookupTag == lowerBound?.lookupTag && !isRaw) {
                return lookupTag.constructClassType(lowerBound.typeArguments, isNullable = true, attributes)
            }

            val mappedTypeArguments = if (isRaw) {
                val defaultArgs = (1..classifier.typeParameters.size).map { ConeStarProjection }

                if (forTypeParameterBounds) {
                    // This is not fully correct, but it's a simple fix for some time to avoid recursive definition:
                    // to create a proper raw type arguments, we should take class parameters some time
                    defaultArgs
                } else {
                    val position = if (lowerBound == null) TypeComponentPosition.FLEXIBLE_LOWER else TypeComponentPosition.FLEXIBLE_UPPER

                    val classSymbol = session.symbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol
                    classSymbol?.fir?.createRawArguments(session, defaultArgs, position) ?: defaultArgs
                }
            } else {
                val typeParameters = runIf(!forTypeParameterBounds && !isForSupertypes) {
                    val classSymbol = session.symbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol
                    classSymbol?.fir?.typeParameters
                } ?: emptyList()

                typeArguments.indices.map { index ->
                    val argument = typeArguments[index]
                    val parameter = typeParameters.getOrNull(index)?.symbol?.fir
                    argument.toConeProjectionWithoutEnhancement(
                        session, javaTypeParameterStack, boundTypeParameter = parameter, isForSupertypes = isForSupertypes
                    )
                }
            }

            lookupTag.constructClassType(mappedTypeArguments.toTypedArray(), isNullable = lowerBound != null, attributes)
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
        (mutableClassId.toLookupTag().toSymbol(session)?.fir as? FirRegularClass)?.typeParameters?.lastOrNull()?.symbol?.fir?.variance
            ?: return false

    return mutableLastParameterVariance != Variance.OUT_VARIANCE
}


private fun FirRegularClass.createRawArguments(
    session: FirSession,
    defaultArgs: List<ConeStarProjection>,
    position: TypeComponentPosition
): List<ConeTypeProjection> = typeParameters.filterIsInstance<FirTypeParameter>().map { typeParameter ->
    val erasedUpperBound = typeParameter.getErasedUpperBound(session) {
        defaultType().withArguments(defaultArgs.toTypedArray())
    }
    computeRawProjection(session, typeParameter, position, erasedUpperBound)
}

private fun JavaType?.toConeProjectionWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    boundTypeParameter: FirTypeParameter?,
    isForSupertypes: Boolean = false
): ConeTypeProjection {
    return when (this) {
        null -> ConeStarProjection
        is JavaWildcardType -> {
            val bound = this.bound
            val argumentVariance = if (isExtends) Variance.OUT_VARIANCE else Variance.IN_VARIANCE
            val parameterVariance = boundTypeParameter?.variance ?: Variance.INVARIANT
            if (bound == null || parameterVariance != Variance.INVARIANT && parameterVariance != argumentVariance) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = isForSupertypes)
                if (argumentVariance == Variance.OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        else -> toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = isForSupertypes)
    }
}
