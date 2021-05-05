/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirReferencePlaceholderForResolvedAnnotations
import org.jetbrains.kotlin.fir.resolve.bindSymbolToLookupTag
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.expectedConeType
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.java.typeEnhancement.TypeComponentPosition
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.lang.Deprecated
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.util.*

internal val JavaModifierListOwner.modality: Modality
    get() = when {
        isAbstract -> Modality.ABSTRACT
        isFinal -> Modality.FINAL
        else -> Modality.OPEN
    }

internal val JavaClass.modality: Modality
    get() = when {
        isSealed -> Modality.SEALED
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

internal fun ClassId.toLookupTag(): ConeClassLikeLookupTag {
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
        is FirJavaTypeRef -> {
            type.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = false)
        }
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
            toConeKotlinTypeForFlexibleBound(session, javaTypeParameterStack, isLowerBound = true, forTypeParameterBounds, isForSupertypes)
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
    isForSupertypes: Boolean = false,
    attributes: ConeAttributes = ConeAttributes.Empty
): ConeKotlinType {
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
                isForSupertypes = isForSupertypes,
                attributes = attributes
            ) ?: run {
                StandardClassIds.Any.toConeFlexibleType(emptyArray())
            }
        null -> {
            StandardClassIds.Any.toConeFlexibleType(emptyArray())
        }
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
    toConeKotlinType(
        typeArguments,
        isNullable = false,
        attributes
    ),
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
        isLowerBound = true,
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
            isLowerBound = false,
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
    isLowerBound: Boolean,
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

            if (isLowerBound) {
                classId = classId.readOnlyToMutable() ?: classId
            }

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            if (!isLowerBound && !isRaw && lookupTag == lowerBound?.lookupTag) {
                return lookupTag.constructClassType(
                    lowerBound.typeArguments, isNullable = true
                )
            }

            val classSymbol = session.symbolProvider.getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol

            val mappedTypeArguments = if (isRaw) {
                val defaultArgs = (1..classifier.typeParameters.size).map { ConeStarProjection }

                if (forTypeParameterBounds) {
                    // This is not fully correct, but it's a simple fix for some time to avoid recursive definition:
                    // to create a proper raw type arguments, we should take class parameters some time
                    defaultArgs
                } else {
                    val position = if (isLowerBound) TypeComponentPosition.FLEXIBLE_LOWER else TypeComponentPosition.FLEXIBLE_UPPER

                    classSymbol?.fir?.createRawArguments(session, defaultArgs, position) ?: defaultArgs
                }
            } else {
                val typeParameters = runIf(!forTypeParameterBounds && !isForSupertypes) { classSymbol?.fir?.typeParameters } ?: emptyList()

                typeArguments.indices.map { index ->
                    val argument = typeArguments[index]
                    val parameter = typeParameters.getOrNull(index)?.symbol?.fir
                    argument.toConeProjectionWithoutEnhancement(
                        session, javaTypeParameterStack, boundTypeParameter = parameter, isForSupertypes = isForSupertypes
                    )
                }
            }

            lookupTag.constructClassType(
                mappedTypeArguments.toTypedArray(), isNullable = !isLowerBound, attributes
            )
        }
        is JavaTypeParameter -> {
            val symbol = javaTypeParameterStack[classifier]
            ConeTypeParameterTypeImpl(symbol.toLookupTag(), isNullable = !isLowerBound)
        }
        else -> ConeKotlinErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
    }
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

private fun buildEnumCall(session: FirSession, classId: ClassId?, entryName: Name?) =
    buildFunctionCall {
        val calleeReference = if (classId != null && entryName != null) {
            session.symbolProvider.getClassDeclaredPropertySymbols(classId, entryName)
                .firstOrNull()?.let { propertySymbol ->
                    buildResolvedNamedReference {
                        name = entryName
                        resolvedSymbol = propertySymbol
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

private val JAVA_TARGET_CLASS_ID = ClassId.topLevel(FqName(Target::class.java.canonicalName))
private val JAVA_RETENTION_CLASS_ID = ClassId.topLevel(FqName(Retention::class.java.canonicalName))
private val JAVA_DEPRECATED_CLASS_ID = ClassId.topLevel(FqName(Deprecated::class.java.canonicalName))
private val JAVA_DOCUMENTED_CLASS_ID = ClassId.topLevel(FqName(Documented::class.java.canonicalName))
private val JAVA_REPEATABLE_CLASS_ID = ClassId.topLevel(FqName("java.lang.annotation.Repeatable")) // since Java 8

private val JAVA_TARGETS_TO_KOTLIN = mapOf(
    "TYPE" to EnumSet.of(AnnotationTarget.CLASS, AnnotationTarget.FILE),
    "ANNOTATION_TYPE" to EnumSet.of(AnnotationTarget.ANNOTATION_CLASS),
    "TYPE_PARAMETER" to EnumSet.of(AnnotationTarget.TYPE_PARAMETER),
    "FIELD" to EnumSet.of(AnnotationTarget.FIELD),
    "LOCAL_VARIABLE" to EnumSet.of(AnnotationTarget.LOCAL_VARIABLE),
    "PARAMETER" to EnumSet.of(AnnotationTarget.VALUE_PARAMETER),
    "CONSTRUCTOR" to EnumSet.of(AnnotationTarget.CONSTRUCTOR),
    "METHOD" to EnumSet.of(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER),
    "TYPE_USE" to EnumSet.of(AnnotationTarget.TYPE)
)

private fun List<JavaAnnotationArgument>.mapJavaTargetArguments(session: FirSession): FirExpression? =
    buildArrayOfCall {
        argumentList = buildArgumentList {
            val resultSet = EnumSet.noneOf(AnnotationTarget::class.java)
            for (target in this@mapJavaTargetArguments) {
                if (target !is JavaEnumValueAnnotationArgument) return null
                resultSet.addAll(JAVA_TARGETS_TO_KOTLIN[target.entryName?.asString()] ?: continue)
            }
            val classId = ClassId.topLevel(StandardNames.FqNames.annotationTarget)
            resultSet.mapTo(arguments) { buildEnumCall(session, classId, Name.identifier(it.name)) }
        }
    }

private val JAVA_RETENTION_TO_KOTLIN = mapOf(
    "RUNTIME" to AnnotationRetention.RUNTIME,
    "CLASS" to AnnotationRetention.BINARY,
    "SOURCE" to AnnotationRetention.SOURCE
)

private fun JavaAnnotationArgument.mapJavaRetentionArgument(session: FirSession): FirExpression? =
    JAVA_RETENTION_TO_KOTLIN[(this as? JavaEnumValueAnnotationArgument)?.entryName?.asString()]?.let {
        buildEnumCall(session, ClassId.topLevel(StandardNames.FqNames.annotationRetention), Name.identifier(it.name))
    }

private fun buildArgumentMapping(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    lookupTag: ConeClassLikeLookupTagImpl,
    annotationArguments: Collection<JavaAnnotationArgument>
): FirArgumentList? {
    if (annotationArguments.none { it.name != null }) {
        return null
    }
    val annotationClassSymbol = session.symbolProvider.getClassLikeSymbolByFqName(lookupTag.classId).also {
        lookupTag.bindSymbolToLookupTag(session, it)
    } ?: return null
    val annotationConstructor =
        (annotationClassSymbol.fir as FirRegularClass).declarations.filterIsInstance<FirConstructor>().first()
    val mapping = annotationArguments.associateTo(linkedMapOf()) { argument ->
        val parameter = annotationConstructor.valueParameters.find { it.name == (argument.name ?: JavaSymbolProvider.VALUE_METHOD_NAME) }
            ?: return null
        argument.toFirExpression(session, javaTypeParameterStack, parameter.returnTypeRef) to parameter
    }
    return buildResolvedArgumentList(mapping)
}

internal fun JavaAnnotation.toFirAnnotationCall(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirAnnotationCall {
    return buildAnnotationCall {
        val lookupTag = when (classId) {
            JAVA_TARGET_CLASS_ID -> ClassId.topLevel(StandardNames.FqNames.target)
            JAVA_RETENTION_CLASS_ID -> ClassId.topLevel(StandardNames.FqNames.retention)
            JAVA_REPEATABLE_CLASS_ID -> ClassId.topLevel(StandardNames.FqNames.repeatable)
            JAVA_DOCUMENTED_CLASS_ID -> ClassId.topLevel(StandardNames.FqNames.mustBeDocumented)
            JAVA_DEPRECATED_CLASS_ID -> ClassId.topLevel(StandardNames.FqNames.deprecated)
            else -> classId
        }?.let(::ConeClassLikeLookupTagImpl)
        annotationTypeRef = if (lookupTag != null) {
            buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(lookupTag, emptyArray(), isNullable = false)
            }
        } else {
            buildErrorTypeRef { diagnostic = ConeUnresolvedReferenceError() }
        }
        argumentList = when (classId) {
            JAVA_TARGET_CLASS_ID -> when (val argument = arguments.singleOrNull()) {
                is JavaArrayAnnotationArgument -> argument.getElements().mapJavaTargetArguments(session)?.let(::buildUnaryArgumentList)
                is JavaEnumValueAnnotationArgument -> listOf(argument).mapJavaTargetArguments(session)?.let(::buildUnaryArgumentList)
                else -> null
            }
            JAVA_RETENTION_CLASS_ID -> arguments.singleOrNull()?.mapJavaRetentionArgument(session)?.let(::buildUnaryArgumentList)
            JAVA_DEPRECATED_CLASS_ID ->
                buildUnaryArgumentList(buildConstExpression(null, ConstantValueKind.String, "Deprecated in Java").setProperType(session))
            null -> null
            else -> buildArgumentMapping(session, javaTypeParameterStack, lookupTag!!, arguments)
        } ?: buildArgumentList {
            this@toFirAnnotationCall.arguments.mapTo(arguments) {
                it.toFirExpression(session, javaTypeParameterStack, null)
            }
        }
        calleeReference = FirReferencePlaceholderForResolvedAnnotations
    }
}

@FirBuilderDsl
internal fun FirAnnotationContainerBuilder.addAnnotationsFrom(
    session: FirSession, javaAnnotationOwner: JavaAnnotationOwner, javaTypeParameterStack: JavaTypeParameterStack
) {
    annotations.addAnnotationsFrom(session, javaAnnotationOwner, javaTypeParameterStack)
}

internal fun FirJavaClass.addAnnotationsFrom(
    session: FirSession, javaAnnotationOwner: JavaAnnotationOwner, javaTypeParameterStack: JavaTypeParameterStack
) {
    annotations.addAnnotationsFrom(session, javaAnnotationOwner, javaTypeParameterStack)
}

internal fun MutableList<FirAnnotationCall>.addAnnotationsFrom(
    session: FirSession,
    javaAnnotationOwner: JavaAnnotationOwner,
    javaTypeParameterStack: JavaTypeParameterStack
) {
    javaAnnotationOwner.annotations.mapTo(this) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
}

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession, index: Int, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return buildJavaValueParameter {
        source = (this@toFirValueParameter as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
        declarationSiteSession = session
        name = this@toFirValueParameter.name ?: Name.identifier("p$index")
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack)
        isVararg = this@toFirValueParameter.isVararg
        annotationBuilder = { annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) }}
    }
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
            val argumentVariance = if (isExtends) OUT_VARIANCE else IN_VARIANCE
            val parameterVariance = boundTypeParameter?.variance ?: INVARIANT
            if (bound == null || parameterVariance != INVARIANT && parameterVariance != argumentVariance) {
                ConeStarProjection
            } else {
                val boundType = bound.toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = isForSupertypes)
                if (argumentVariance == OUT_VARIANCE) {
                    ConeKotlinTypeProjectionOut(boundType)
                } else {
                    ConeKotlinTypeProjectionIn(boundType)
                }
            }
        }
        is JavaClassifierType -> toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = isForSupertypes)
        is JavaArrayType -> toConeKotlinTypeWithoutEnhancement(session, javaTypeParameterStack, isForSupertypes = isForSupertypes)
        else -> ConeClassErrorType(ConeSimpleDiagnostic("Unexpected type argument: $this", DiagnosticKind.Java))
    }
}

internal fun JavaAnnotationArgument.toFirExpression(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack, expectedTypeRef: FirTypeRef?
): FirExpression {
    return when (this) {
        is JavaLiteralAnnotationArgument -> value.createConstantOrError(session)
        is JavaArrayAnnotationArgument -> buildArrayOfCall {
            val argumentTypeRef = expectedTypeRef?.let {
                typeRef = it
                buildResolvedTypeRef {
                    type = it.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible()?.arrayElementType()
                        ?: ConeClassErrorType(ConeSimpleDiagnostic("expected type is not array type"))
                }
            }
            argumentList = buildArgumentList {
                getElements().mapTo(arguments) { it.toFirExpression(session, javaTypeParameterStack, argumentTypeRef) }
            }
        }
        is JavaEnumValueAnnotationArgument -> buildEnumCall(session, enumClassId, entryName)
        is JavaClassObjectAnnotationArgument -> buildGetClassCall {
            argumentList = buildUnaryArgumentList(
                buildClassReferenceExpression {
                    classTypeRef = getReferencedType().toFirResolvedTypeRef(session, javaTypeParameterStack)
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
private fun <T> List<T>.createArrayOfCall(session: FirSession, @Suppress("UNUSED_PARAMETER") kind: ConstantValueKind<T>): FirArrayOfCall {
    return buildArrayOfCall {
        argumentList = buildArgumentList {
            for (element in this@createArrayOfCall) {
                arguments += element.createConstantOrError(session)
            }
        }
        typeRef = buildResolvedTypeRef {
            type = kind.expectedConeType(session).createArrayType()
        }
    }
}

internal fun Any?.createConstantOrError(session: FirSession): FirExpression {
    return createConstantIfAny(session) ?: buildErrorExpression {
        diagnostic = ConeSimpleDiagnostic("Unknown value in JavaLiteralAnnotationArgument: $this", DiagnosticKind.Java)
    }
}

internal fun Any?.createConstantIfAny(session: FirSession): FirExpression? {
    return when (this) {
        is Byte -> buildConstExpression(null, ConstantValueKind.Byte, this).setProperType(session)
        is Short -> buildConstExpression(null, ConstantValueKind.Short, this).setProperType(session)
        is Int -> buildConstExpression(null, ConstantValueKind.Int, this).setProperType(session)
        is Long -> buildConstExpression(null, ConstantValueKind.Long, this).setProperType(session)
        is Char -> buildConstExpression(null, ConstantValueKind.Char, this).setProperType(session)
        is Float -> buildConstExpression(null, ConstantValueKind.Float, this).setProperType(session)
        is Double -> buildConstExpression(null, ConstantValueKind.Double, this).setProperType(session)
        is Boolean -> buildConstExpression(null, ConstantValueKind.Boolean, this).setProperType(session)
        is String -> buildConstExpression(null, ConstantValueKind.String, this).setProperType(session)
        is ByteArray -> toList().createArrayOfCall(session, ConstantValueKind.Byte)
        is ShortArray -> toList().createArrayOfCall(session, ConstantValueKind.Short)
        is IntArray -> toList().createArrayOfCall(session, ConstantValueKind.Int)
        is LongArray -> toList().createArrayOfCall(session, ConstantValueKind.Long)
        is CharArray -> toList().createArrayOfCall(session, ConstantValueKind.Char)
        is FloatArray -> toList().createArrayOfCall(session, ConstantValueKind.Float)
        is DoubleArray -> toList().createArrayOfCall(session, ConstantValueKind.Double)
        is BooleanArray -> toList().createArrayOfCall(session, ConstantValueKind.Boolean)
        null -> buildConstExpression(null, ConstantValueKind.Null, null).setProperType(session)

        else -> null
    }
}

private fun FirConstExpression<*>.setProperType(session: FirSession): FirConstExpression<*> {
    val typeRef = buildResolvedTypeRef {
        type = kind.expectedConeType(session)
    }
    replaceTypeRef(typeRef)
    session.lookupTracker?.recordTypeResolveAsLookup(typeRef, source, null)
    return this
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
        type = ConeClassErrorType(ConeSimpleDiagnostic("Unexpected JavaType: $this", DiagnosticKind.Java))
    }
}
