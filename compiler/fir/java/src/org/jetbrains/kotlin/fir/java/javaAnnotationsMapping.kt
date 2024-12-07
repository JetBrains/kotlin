/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirAnnotationArgumentMappingImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.java.declarations.buildJavaExternalAnnotation
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.java.enhancement.FirLazyJavaAnnotationList
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*

internal fun Iterable<JavaAnnotation>.convertAnnotationsToFir(
    session: FirSession, source: KtSourceElement?,
): List<FirAnnotation> = map { it.toFirAnnotationCall(session, source) }

internal fun Iterable<JavaAnnotation>.convertAnnotationsToFir(
    session: FirSession,
    source: KtSourceElement?,
    isDeprecatedInJavaDoc: Boolean,
): List<FirAnnotation> {
    var annotationWithJavaTarget: FirAnnotation? = null
    var annotationWithKotlinTarget: FirAnnotation? = null
    val result = buildList {
        var isDeprecated = false

        this@convertAnnotationsToFir.mapTo(this) {
            if (it.isJavaDeprecatedAnnotation()) isDeprecated = true
            val firAnnotationCall = it.toFirAnnotationCall(session, source)
            if (firAnnotationCall.toAnnotationClassId(session) == StandardClassIds.Annotations.Target) {
                val unmappedKotlinAnnotation = it.classId == StandardClassIds.Annotations.Target
                if (annotationWithJavaTarget == null && !unmappedKotlinAnnotation) {
                    annotationWithJavaTarget = firAnnotationCall
                }
                if (annotationWithKotlinTarget == null && unmappedKotlinAnnotation) {
                    annotationWithKotlinTarget = firAnnotationCall
                }
            }
            firAnnotationCall
        }

        if (!isDeprecated && isDeprecatedInJavaDoc) {
            add(DeprecatedInJavaDocAnnotation.toFirAnnotationCall(session, source))
        }
    }
    if (annotationWithKotlinTarget == null) return result

    // TODO: remove after K1 build no more needed
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    return result.mergeTargetAnnotations(annotationWithJavaTarget, annotationWithKotlinTarget!!)
}

// Special code for situation with java.lang.annotation.Target and kotlin.annotation.Target together
private fun List<FirAnnotation>.mergeTargetAnnotations(
    annotationWithJavaTarget: FirAnnotation?,
    annotationWithKotlinTarget: FirAnnotation,
): List<FirAnnotation> {
    return filter { it !== annotationWithJavaTarget && it !== annotationWithKotlinTarget } +
            buildAnnotationCopy(annotationWithKotlinTarget) {
                argumentMapping = buildAnnotationArgumentMapping {
                    this.source = annotationWithKotlinTarget.argumentMapping.source
                    mapping[StandardClassIds.Annotations.ParameterNames.targetAllowedTargets] = buildVarargArgumentsExpressionWithTargets {
                        arguments += if (annotationWithJavaTarget == null) {
                            JAVA_DEFAULT_TARGET_SET.map {
                                buildEnumEntryDeserializedAccessExpression {
                                    enumClassId = StandardClassIds.AnnotationTarget
                                    enumEntryName = Name.identifier(it.name)
                                }
                            }
                        } else {
                            annotationWithJavaTarget.targetArgumentExpressions()
                        }
                        arguments += annotationWithKotlinTarget.targetArgumentExpressions()
                    }
                }
            }
}

inline fun buildVarargArgumentsExpressionWithTargets(
    init: FirVarargArgumentsExpressionBuilder.() -> Unit = {},
): FirVarargArgumentsExpression {
    return FirVarargArgumentsExpressionBuilder().apply {
        init()
        val elementConeType = ConeClassLikeTypeImpl(
            StandardClassIds.AnnotationTarget.toLookupTag(),
            emptyArray(),
            isMarkedNullable = false,
            ConeAttributes.Empty
        )
        coneTypeOrNull = elementConeType.createOutArrayType()
        coneElementTypeOrNull = elementConeType
    }.build()
}

private fun FirAnnotation.targetArgumentExpressions(): List<FirExpression> =
    when (val mapped = argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.targetAllowedTargets]) {
        is FirVarargArgumentsExpression -> mapped.arguments
        is FirArrayLiteral -> mapped.argumentList.arguments
        else -> listOf(this)
    }

internal fun JavaAnnotationOwner.convertAnnotationsToFir(
    session: FirSession, source: KtSourceElement?,
): List<FirAnnotation> = annotations.convertAnnotationsToFir(session, source, isDeprecatedInJavaDoc)

internal object DeprecatedInJavaDocAnnotation : JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument> get() = emptyList()
    override val classId: ClassId get() = JvmStandardClassIds.Annotations.Java.Deprecated
    override fun resolve(): JavaClass? = null
}

internal fun FirAnnotationContainer.setAnnotationsFromJava(
    session: FirSession, source: KtSourceElement?,
    javaAnnotationOwner: JavaAnnotationOwner,
) {
    val annotations = mutableListOf<FirAnnotation>()
    javaAnnotationOwner.annotations.mapTo(annotations) { it.toFirAnnotationCall(session, source) }
    replaceAnnotations(annotations)
}

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession,
    functionSymbol: FirFunctionSymbol<*>,
    moduleData: FirModuleData,
    index: Int,
): FirValueParameter = buildJavaValueParameter {
    source = toSourceElement()
    isFromSource = this@toFirValueParameter.isFromSource
    this.moduleData = moduleData
    containingDeclarationSymbol = functionSymbol
    name = this@toFirValueParameter.name ?: Name.identifier("p$index")
    returnTypeRef = type.toFirJavaTypeRef(session, source)
    isVararg = this@toFirValueParameter.isVararg
    annotationList = FirLazyJavaAnnotationList(this@toFirValueParameter, moduleData)
}

internal fun JavaAnnotationArgument.toFirExpression(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack, expectedTypeRef: FirTypeRef?,
    source: KtSourceElement?,
): FirExpression {
    val expectedConeType = expectedTypeRef?.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack, source)
    val expectedArrayElementTypeIfArray = expectedConeType?.lowerBoundIfFlexible()?.arrayElementType() ?: expectedConeType
    val argument = when (this) {
        is JavaLiteralAnnotationArgument -> value.createConstantOrError(
            session,
            expectedArrayElementTypeIfArray
        )
        is JavaArrayAnnotationArgument -> buildArrayLiteral {
            val argumentTypeRef = expectedConeType?.let {
                coneTypeOrNull = it
                buildResolvedTypeRef {
                    this.coneType = it.lowerBoundIfFlexible().arrayElementType()
                        ?: ConeErrorType(ConeSimpleDiagnostic("expected type is not array type"))
                }
            }
            argumentList = buildArgumentList {
                getElements().mapTo(arguments) { it.toFirExpression(session, javaTypeParameterStack, argumentTypeRef, source) }
            }
        }
        is JavaEnumValueAnnotationArgument -> {
            val classId = requireNotNull(enumClassId ?: expectedArrayElementTypeIfArray?.lowerBoundIfFlexible()?.classId)
            buildEnumEntryDeserializedAccessExpression {
                // enumClassId can be null when a java annotation uses a Kotlin enum as parameter and declares the default value using
                // a static import. In this case, the parameter default initializer will not have its type set, which isn't usually an
                // issue except in edge cases like KT-47702 where we do need to evaluate the default values of annotations.
                // As a fallback, we use the expected type which should be the type of the enum.
                enumClassId = classId
                enumEntryName = entryName ?: SpecialNames.NO_NAME_PROVIDED
            }
        }
        is JavaClassObjectAnnotationArgument -> buildGetClassCall {
            val resolvedClassTypeRef = getReferencedType().toFirResolvedTypeRef(session, javaTypeParameterStack, source)
            val resolvedTypeRef = buildResolvedTypeRef {
                coneType = StandardClassIds.KClass.constructClassLikeType(arrayOf(resolvedClassTypeRef.coneType), false)
            }
            argumentList = buildUnaryArgumentList(
                buildClassReferenceExpression {
                    classTypeRef = resolvedClassTypeRef
                    coneTypeOrNull = resolvedTypeRef.coneType
                }
            )
            coneTypeOrNull = resolvedTypeRef.coneType
        }
        is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall(session, source)
        else -> buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("Unknown JavaAnnotationArgument: ${this::class.java}", DiagnosticKind.Java)
        }
    }

    return if (expectedConeType?.lowerBoundIfFlexible()?.isArrayOrPrimitiveArray == true && argument !is FirArrayLiteral) {
        buildArrayLiteral {
            coneTypeOrNull = expectedConeType
            argumentList = buildArgumentList {
                arguments += argument
            }
        }
    } else {
        argument
    }
}

private val JAVA_RETENTION_TO_KOTLIN: Map<String, AnnotationRetention> = mapOf(
    "RUNTIME" to AnnotationRetention.RUNTIME,
    "CLASS" to AnnotationRetention.BINARY,
    "SOURCE" to AnnotationRetention.SOURCE
)

private val JAVA_TARGETS_TO_KOTLIN: Map<String, EnumSet<AnnotationTarget>> = mapOf(
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

private val JAVA_DEFAULT_TARGET_SET: Set<KotlinTarget> = KotlinTarget.DEFAULT_TARGET_SET - KotlinTarget.PROPERTY

private fun List<JavaAnnotationArgument>.mapJavaTargetArguments(): FirExpression? {
    return buildVarargArgumentsExpressionWithTargets {
        val resultSet = EnumSet.noneOf(AnnotationTarget::class.java)
        for (target in this@mapJavaTargetArguments) {
            if (target !is JavaEnumValueAnnotationArgument) return null
            resultSet.addAll(JAVA_TARGETS_TO_KOTLIN[target.entryName?.asString()] ?: continue)
        }
        resultSet.mapTo(arguments) {
            buildEnumEntryDeserializedAccessExpression {
                enumClassId = StandardClassIds.AnnotationTarget
                enumEntryName = Name.identifier(it.name)
            }
        }
    }
}

private fun JavaAnnotationArgument.mapJavaRetentionArgument(): FirExpression? {
    return JAVA_RETENTION_TO_KOTLIN[(this as? JavaEnumValueAnnotationArgument)?.entryName?.asString()]?.let {
        buildEnumEntryDeserializedAccessExpression {
            enumClassId = StandardClassIds.AnnotationRetention
            enumEntryName = Name.identifier(it.name)
        }
    }
}

private fun fillAnnotationArgumentMapping(
    session: FirSession,
    lookupTag: ConeClassLikeLookupTagImpl,
    annotationArguments: Collection<JavaAnnotationArgument>,
    destination: MutableMap<Name, FirExpression>,
    source: KtSourceElement?,
) {
    if (annotationArguments.isEmpty()) return

    val annotationClassSymbol = lookupTag.toSymbol(session)
    val annotationConstructor = (annotationClassSymbol?.fir as FirRegularClass?)
        ?.declarations
        ?.firstIsInstanceOrNull<FirConstructor>()
    annotationArguments.associateTo(destination) { argument ->
        val name = argument.name ?: StandardClassIds.Annotations.ParameterNames.value
        val parameter = annotationConstructor?.valueParameters?.find { it.name == name }
        name to argument.toFirExpression(session, JavaTypeParameterStack.EMPTY, parameter?.returnTypeRef, source)
    }
}

internal fun JavaAnnotation.isJavaDeprecatedAnnotation(): Boolean {
    return classId == JvmStandardClassIds.Annotations.Java.Deprecated
}

private fun JavaAnnotation.toFirAnnotationCall(session: FirSession, source: KtSourceElement?): FirAnnotation {
    val annotationData = buildFirAnnotation(this, session, source)
    return if (isIdeExternalAnnotation) {
        buildJavaExternalAnnotation {
            annotationTypeRef = annotationData.annotationTypeRef
            argumentMapping = annotationData.argumentsMapping
        }
    } else {
        buildAnnotation {
            annotationTypeRef = annotationData.annotationTypeRef
            argumentMapping = annotationData.argumentsMapping
            this.source = source
        }
    }
}

private class AnnotationData(val annotationTypeRef: FirResolvedTypeRef, val argumentsMapping: FirAnnotationArgumentMapping)

private fun buildFirAnnotation(
    javaAnnotation: JavaAnnotation,
    session: FirSession,
    source: KtSourceElement?,
): AnnotationData {
    val classId = javaAnnotation.classId
    val lookupTag = when (classId) {
        JvmStandardClassIds.Annotations.Java.Target -> StandardClassIds.Annotations.Target
        JvmStandardClassIds.Annotations.Java.Retention -> StandardClassIds.Annotations.Retention
        JvmStandardClassIds.Annotations.Java.Documented -> StandardClassIds.Annotations.MustBeDocumented
        JvmStandardClassIds.Annotations.Java.Deprecated -> StandardClassIds.Annotations.Deprecated
        else -> classId
    }?.toLookupTag()
    val annotationTypeRef = if (lookupTag != null) {
        buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(lookupTag, emptyArray(), isMarkedNullable = false)
            this.source = source
        }
    } else {
        val unresolvedName = classId?.shortClassName ?: SpecialNames.NO_NAME_PROVIDED
        buildErrorTypeRef {
            diagnostic = ConeUnresolvedReferenceError(unresolvedName)
            this.source = source
        }
    }

    /**
     * This is required to avoid contract violation during [org.jetbrains.kotlin.fir.declarations.getOwnDeprecationForCallSite]
     * Because argument transformation may lead to [org.jetbrains.kotlin.fir.declarations.FirResolvePhase.TYPES]+ lazy resolution
     * See KT-59342
     * TODO: KT-60520
     */
    val argumentMapping = when {
        lookupTag == null || classId == JvmStandardClassIds.Annotations.Java.Documented -> FirEmptyAnnotationArgumentMapping
        classId == JvmStandardClassIds.Annotations.Java.Deprecated -> {
            FirAnnotationArgumentMappingImpl(
                source = null,
                mapping = mapOf(
                    StandardClassIds.Annotations.ParameterNames.deprecatedMessage to "Deprecated in Java".createConstantOrError(session)
                )
            )
        }
        else -> object : FirAnnotationArgumentMapping() {
            override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
            override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
            override val source: KtSourceElement? get() = null

            override val mapping: Map<Name, FirExpression> by lazy {
                when (classId) {
                    JvmStandardClassIds.Annotations.Java.Target -> {
                        when (val argument = javaAnnotation.arguments.firstOrNull()) {
                            is JavaArrayAnnotationArgument -> argument.getElements().mapJavaTargetArguments()
                            is JavaEnumValueAnnotationArgument -> listOf(argument).mapJavaTargetArguments()
                            else -> null
                        }?.let {
                            mapOf(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets to it)
                        }
                    }

                    JvmStandardClassIds.Annotations.Java.Retention -> {
                        javaAnnotation.arguments.firstOrNull()?.mapJavaRetentionArgument()?.let {
                            mapOf(StandardClassIds.Annotations.ParameterNames.retentionValue to it)
                        }
                    }

                    else -> javaAnnotation.arguments.ifNotEmpty {
                        val mapping = LinkedHashMap<Name, FirExpression>(size)
                        fillAnnotationArgumentMapping(session, lookupTag, this, mapping, source)
                        mapping
                    }
                }.orEmpty()
            }
        }
    }

    return AnnotationData(annotationTypeRef, argumentMapping)
}
