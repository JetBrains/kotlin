/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.java.typeEnhancement.TypeComponentPosition
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance.*

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

internal val JavaClass.classKind: ClassKind
    get() = when {
        isAnnotationType -> ClassKind.ANNOTATION_CLASS
        isInterface -> ClassKind.INTERFACE
        isEnum -> ClassKind.ENUM_CLASS
        else -> ClassKind.CLASS
    }

internal fun ClassId.toConeKotlinType(
    typeArguments: Array<ConeTypeProjection>,
    isNullable: Boolean
): ConeLookupTagBasedType {
    val lookupTag = ConeClassLikeLookupTagImpl(this)
    return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable)
}

internal fun FirTypeRef.toConeKotlinTypeProbablyFlexible(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType =
    when (this) {
        is FirResolvedTypeRef -> type
        is FirJavaTypeRef -> {
            type.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
        }
        else -> ConeKotlinErrorType("Unexpected type reference in JavaClassUseSiteMemberScope: ${this::class.java}")
    }

internal fun JavaType.toFirJavaTypeRef(session: FirSession, javaTypeParameterStack: JavaTypeParameterStack): FirJavaTypeRef {
    val annotations = (this as? JavaClassifierType)?.annotations.orEmpty()
    return buildJavaTypeRef {
        annotations.mapTo(this.annotations) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
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
            toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, isLowerBound = true, forTypeParameterBounds)
        else
            toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, forTypeParameterBounds)

    return buildResolvedTypeRef {
        type = coneType
        this@toFirResolvedTypeRef.annotations.mapTo(annotations) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
    }
}

internal fun JavaType?.toConeKotlinTypeWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack
): ConeKotlinType {
    return when (this) {
        is JavaClassifierType -> {
            toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
        }
        is JavaPrimitiveType -> {
            val primitiveType = type
            val kotlinPrimitiveName = when (val javaName = primitiveType?.typeName?.asString()) {
                null -> "Unit"
                else -> javaName.capitalize()
            }

            val classId = StandardClassIds.byName(kotlinPrimitiveName)
            classId.toConeKotlinType(emptyArray(), isNullable = false)
        }
        is JavaArrayType -> {
            val componentType = componentType
            if (componentType !is JavaPrimitiveType) {
                val classId = StandardClassIds.Array
                val argumentType = componentType.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
                classId.toConeFlexibleType(
                    arrayOf(argumentType),
                    typeArgumentsForUpper = arrayOf(ConeKotlinTypeProjectionOut(argumentType))
                )
            } else {
                val javaComponentName = componentType.type?.typeName?.asString()?.capitalize() ?: error("Array of voids")
                val classId = StandardClassIds.byName(javaComponentName + "Array")

                classId.toConeFlexibleType(emptyArray())
            }
        }
        is JavaWildcardType -> bound?.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack) ?: run {
            StandardClassIds.Any.toConeFlexibleType(emptyArray())
        }
        null -> {
            StandardClassIds.Any.toConeFlexibleType(emptyArray())
        }
        else -> error("Strange JavaType: ${this::class.java}")
    }
}

private fun ClassId.toConeFlexibleType(
    typeArguments: Array<ConeTypeProjection>,
    typeArgumentsForUpper: Array<ConeTypeProjection> = typeArguments
) = ConeFlexibleType(
    toConeKotlinType(typeArguments, isNullable = false),
    toConeKotlinType(typeArgumentsForUpper, isNullable = true)
)

private fun JavaClassifierType.toConeKotlinTypeWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    forTypeParameterBounds: Boolean = false
): ConeKotlinType {
    val lowerBound = toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, isLowerBound = true, forTypeParameterBounds)
    val upperBound =
        toConeKotlinTypeForFlexibleBound(
            session, javaTypeParameterStack, isLowerBound = false, forTypeParameterBounds, lowerBound
        )

    return if (isRaw) ConeRawType(lowerBound, upperBound) else ConeFlexibleType(lowerBound, upperBound)
}

private fun computeRawProjection(
    session: FirSession,
    parameter: FirTypeParameter,
    attr: TypeComponentPosition,
    erasedUpperBound: ConeKotlinType = parameter.getErasedUpperBound()
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
    // Calculation of `potentiallyRecursiveTypeParameter.upperBounds` may recursively depend on `this.getErasedUpperBound`
    // E.g. `class A<T extends A, F extends A>`
    // To prevent recursive calls return defaultValue() instead
    potentiallyRecursiveTypeParameter: FirTypeParameter? = null,
    defaultValue: (() -> ConeKotlinType) = { ConeKotlinErrorType("Can't compute erased upper bound of type parameter `$this`") }
): ConeKotlinType {
    if (this === potentiallyRecursiveTypeParameter) return defaultValue()

    val firstUpperBound = this.bounds.first().coneTypeUnsafe<ConeKotlinType>()

    return getErasedVersionOfFirstUpperBound(firstUpperBound, mutableSetOf(this, potentiallyRecursiveTypeParameter), defaultValue)
}

private fun getErasedVersionOfFirstUpperBound(
    firstUpperBound: ConeKotlinType,
    alreadyVisitedParameters: MutableSet<FirTypeParameter?>,
    defaultValue: () -> ConeKotlinType
): ConeKotlinType =
    when (firstUpperBound) {
        is ConeClassLikeType ->
            firstUpperBound.withArguments(firstUpperBound.typeArguments.map { ConeStarProjection }.toTypedArray())

        is ConeFlexibleType -> {
            val lowerBound =
                getErasedVersionOfFirstUpperBound(firstUpperBound.lowerBound, alreadyVisitedParameters, defaultValue)
                    .lowerBoundIfFlexible()
            if (firstUpperBound.upperBound is ConeTypeParameterType) {
                // Avoid exponential complexity
                ConeFlexibleType(
                    lowerBound,
                    lowerBound.withNullability(ConeNullability.NULLABLE)
                )
            } else {
                ConeFlexibleType(
                    lowerBound,
                    getErasedVersionOfFirstUpperBound(firstUpperBound.upperBound, alreadyVisitedParameters, defaultValue)
                )
            }
        }
        is ConeTypeParameterType -> {
            val current = firstUpperBound.lookupTag.typeParameterSymbol.fir

            if (alreadyVisitedParameters.add(current)) {
                val nextUpperBound = current.bounds.first().coneTypeUnsafe<ConeKotlinType>()
                getErasedVersionOfFirstUpperBound(nextUpperBound, alreadyVisitedParameters, defaultValue)
            } else {
                defaultValue()
            }
        }
        else -> error("Unexpected kind of firstUpperBound: $firstUpperBound [${firstUpperBound::class}]")
    }

private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    isLowerBound: Boolean,
    forTypeParameterBounds: Boolean,
    lowerBound: ConeLookupTagBasedType? = null
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            //val classId = classifier.classId!!
            var classId = JavaToKotlinClassMap.mapJavaToKotlin(classifier.fqName!!) ?: classifier.classId!!

            if (isLowerBound) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            if (!isLowerBound && !isRaw && lookupTag == lowerBound?.lookupTag) {
                return lookupTag.constructClassType(
                    lowerBound.typeArguments, isNullable = true
                )
            }

            val mappedTypeArguments = if (isRaw) {

                val defaultArgs = (1..classifier.typeParameters.size).map { ConeStarProjection }

                if (forTypeParameterBounds) {
                    // This is not fully correct, but it's a simple fix for some time to avoid recursive definition:
                    // to create a proper raw type arguments, we should take class parameters some time
                    defaultArgs
                } else {
                    val classSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol
                    val position = if (isLowerBound) TypeComponentPosition.FLEXIBLE_LOWER else TypeComponentPosition.FLEXIBLE_UPPER

                    classSymbol?.fir?.createRawArguments(defaultArgs, position) ?: defaultArgs
                }
            } else {
                typeArguments.map { argument ->
                    argument.toConeProjectionWithoutEnhancement(
                        session, javaTypeParameterStack, boundTypeParameter = null
                    )
                }
            }

            lookupTag.constructClassType(
                mappedTypeArguments.toTypedArray(), isNullable = !isLowerBound
            )
        }
        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            ConeTypeParameterTypeImpl(symbol.toLookupTag(), isNullable = !isLowerBound)
        }
        else -> ConeKotlinErrorType("Unexpected classifier: $classifier")
    }
}

private fun FirRegularClass.createRawArguments(
    defaultArgs: List<ConeStarProjection>,
    position: TypeComponentPosition
) = typeParameters.map { typeParameter ->
    val erasedUpperBound = typeParameter.getErasedUpperBound {
        defaultType().withArguments(defaultArgs.toTypedArray())
    }
    computeRawProjection(session, typeParameter, position, erasedUpperBound)
}

internal fun JavaAnnotation.toFirAnnotationCall(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirAnnotationCall {
    return buildAnnotationCall {
        annotationTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(FirRegularClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false)
        }
        argumentList = buildArgumentList {
            for (argument in this@toFirAnnotationCall.arguments) {
                arguments += argument.toFirExpression(session, javaTypeParameterStack)
            }
        }
    }
}

@FirBuilderDsl
internal fun FirAnnotationContainerBuilder.addAnnotationsFrom(
    session: FirSession, javaAnnotationOwner: JavaAnnotationOwner, javaTypeParameterStack: JavaTypeParameterStack
) {
    for (annotation in javaAnnotationOwner.annotations) {
        annotations += annotation.toFirAnnotationCall(session, javaTypeParameterStack)
    }
}

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession, index: Int, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return buildJavaValueParameter {
        source = (this@toFirValueParameter as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
        this.session = session
        name = this@toFirValueParameter.name ?: Name.identifier("p$index")
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack)
        isVararg = this@toFirValueParameter.isVararg
        addAnnotationsFrom(session, this@toFirValueParameter, javaTypeParameterStack)
    }
}

private fun JavaType?.toConeProjectionWithoutEnhancement(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    boundTypeParameter: FirTypeParameter?
): ConeTypeProjection {
    return when (this) {
        null -> ConeStarProjection
        is JavaWildcardType -> {
            val bound = this.bound
            val argumentVariance = if (isExtends) OUT_VARIANCE else IN_VARIANCE
            val parameterVariance = boundTypeParameter?.variance ?: INVARIANT
            if (bound == null || parameterVariance != INVARIANT && parameterVariance != argumentVariance) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
                if (argumentVariance == OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        is JavaClassifierType -> toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack)
        else -> ConeClassErrorType("Unexpected type argument: $this")
    }
}

private fun JavaAnnotationArgument.toFirExpression(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirExpression {
    // TODO: this.name
    return when (this) {
        is JavaLiteralAnnotationArgument -> {
            value.createConstant(session)
        }
        is JavaArrayAnnotationArgument -> buildArrayOfCall {
            argumentList = buildArgumentList {
                for (element in getElements()) {
                    arguments += element.toFirExpression(session, javaTypeParameterStack)
                }
            }
        }
        is JavaEnumValueAnnotationArgument -> {
            buildFunctionCall {
                val classId = this@toFirExpression.enumClassId
                val entryName = this@toFirExpression.entryName
                val calleeReference = if (classId != null && entryName != null) {
                    val callableSymbol = session.firSymbolProvider.getClassDeclaredCallableSymbols(
                        classId, entryName
                    ).firstOrNull()
                    callableSymbol?.let {
                        buildResolvedNamedReference {
                            name = entryName
                            resolvedSymbol = it
                        }
                    }
                } else {
                    null
                }
                this.calleeReference = calleeReference
                    ?: buildErrorNamedReference {
                        diagnostic = ConeSimpleDiagnostic("Strange Java enum value: $classId.$entryName", DiagnosticKind.Java)
                    }
            }
        }
        is JavaClassObjectAnnotationArgument -> buildGetClassCall {
            val referencedType = getReferencedType()
            argumentList = buildUnaryArgumentList(
                buildClassReferenceExpression {
                    classTypeRef = referencedType.toFirResolvedTypeRef(session, javaTypeParameterStack)
                }
            )
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session, javaTypeParameterStack)
        else -> buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("Unknown JavaAnnotationArgument: ${this::class.java}", DiagnosticKind.Java)
        }
    }
}

// TODO: use kind here
private fun <T> List<T>.createArrayOfCall(session: FirSession, @Suppress("UNUSED_PARAMETER") kind: FirConstKind<T>): FirArrayOfCall {
    return buildArrayOfCall {
        argumentList = buildArgumentList {
            for (element in this@createArrayOfCall) {
                arguments += element.createConstant(session)
            }
        }
    }
}

internal fun Any?.createConstant(session: FirSession): FirExpression {
    return when (this) {
        is Byte -> buildConstExpression(null, FirConstKind.Byte, this)
        is Short -> buildConstExpression(null, FirConstKind.Short, this)
        is Int -> buildConstExpression(null, FirConstKind.Int, this)
        is Long -> buildConstExpression(null, FirConstKind.Long, this)
        is Char -> buildConstExpression(null, FirConstKind.Char, this)
        is Float -> buildConstExpression(null, FirConstKind.Float, this)
        is Double -> buildConstExpression(null, FirConstKind.Double, this)
        is Boolean -> buildConstExpression(null, FirConstKind.Boolean, this)
        is String -> buildConstExpression(null, FirConstKind.String, this)
        is ByteArray -> toList().createArrayOfCall(session, FirConstKind.Byte)
        is ShortArray -> toList().createArrayOfCall(session, FirConstKind.Short)
        is IntArray -> toList().createArrayOfCall(session, FirConstKind.Int)
        is LongArray -> toList().createArrayOfCall(session, FirConstKind.Long)
        is CharArray -> toList().createArrayOfCall(session, FirConstKind.Char)
        is FloatArray -> toList().createArrayOfCall(session, FirConstKind.Float)
        is DoubleArray -> toList().createArrayOfCall(session, FirConstKind.Double)
        is BooleanArray -> toList().createArrayOfCall(session, FirConstKind.Boolean)
        null -> buildConstExpression(null, FirConstKind.Null, null)

        else -> buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
        }
    }
}

private fun JavaType.toFirResolvedTypeRef(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirResolvedTypeRef {
    if (this is JavaClassifierType) return toFirResolvedTypeRef(
        session,
        javaTypeParameterStack,
        isForSupertypes = false,
        forTypeParameterBounds = false
    )
    return buildResolvedTypeRef {
        type = ConeClassErrorType("Unexpected JavaType: $this")
    }
}

