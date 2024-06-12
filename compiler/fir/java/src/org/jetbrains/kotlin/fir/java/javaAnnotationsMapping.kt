/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.buildJavaExternalAnnotation
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.resolve.bindSymbolToLookupTag
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
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.butIf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*

internal fun Iterable<JavaAnnotation>.convertAnnotationsToFir(
    session: FirSession, source: KtSourceElement?
): List<FirAnnotation> = map { it.toFirAnnotationCall(session, source) }

internal fun Iterable<JavaAnnotation>.convertAnnotationsToFir(
    session: FirSession,
    source: KtSourceElement?,
    isDeprecatedInJavaDoc: Boolean,
): List<FirAnnotation> = buildList {
    var isDeprecated = false

    this@convertAnnotationsToFir.mapTo(this) {
        if (it.isJavaDeprecatedAnnotation()) isDeprecated = true
        it.toFirAnnotationCall(session, source)
    }

    if (!isDeprecated && isDeprecatedInJavaDoc) {
        add(DeprecatedInJavaDocAnnotation.toFirAnnotationCall(session, source))
    }
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
    source = (this@toFirValueParameter as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
    isFromSource = this@toFirValueParameter.isFromSource
    this.moduleData = moduleData
    containingFunctionSymbol = functionSymbol
    name = this@toFirValueParameter.name ?: Name.identifier("p$index")
    returnTypeRef = type.toFirJavaTypeRef(session, source)
    isVararg = this@toFirValueParameter.isVararg
    annotationBuilder = { convertAnnotationsToFir(session, source) }
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
                    this.type = it.lowerBoundIfFlexible().arrayElementType()
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
                type = StandardClassIds.KClass.constructClassLikeType(arrayOf(resolvedClassTypeRef.type), false)
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

private fun List<JavaAnnotationArgument>.mapJavaTargetArguments(): FirExpression? {
    return buildVarargArgumentsExpression {
        val resultSet = EnumSet.noneOf(AnnotationTarget::class.java)
        for (target in this@mapJavaTargetArguments) {
            if (target !is JavaEnumValueAnnotationArgument) return null
            resultSet.addAll(JAVA_TARGETS_TO_KOTLIN[target.entryName?.asString()] ?: continue)
        }
        val classId = StandardClassIds.AnnotationTarget
        resultSet.mapTo(arguments) {
            buildEnumEntryDeserializedAccessExpression {
                enumClassId = classId
                enumEntryName = Name.identifier(it.name)
            }
        }
        val elementConeType = ConeClassLikeTypeImpl(
            classId.toLookupTag(),
            emptyArray(),
            isNullable = false,
            ConeAttributes.Empty
        )
        coneTypeOrNull = elementConeType
        coneElementTypeOrNull = elementConeType.createOutArrayType()
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

    val annotationClassSymbol = lookupTag.toSymbol(session).also {
        lookupTag.bindSymbolToLookupTag(session, it)
    }
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
    val isJavaAnnotationMappedToKotlin = lookupTag != classId?.toLookupTag()
    val sourceForTypeRef = source.butIf(isJavaAnnotationMappedToKotlin) {
        it?.fakeElement(KtFakeSourceElementKind.JavaAnnotationMappedToKotlin)
    }
    val annotationTypeRef = if (lookupTag != null) {
        buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(lookupTag, emptyArray(), isNullable = false)
            this.source = sourceForTypeRef
        }
    } else {
        val unresolvedName = classId?.shortClassName ?: SpecialNames.NO_NAME_PROVIDED
        buildErrorTypeRef {
            diagnostic = ConeUnresolvedReferenceError(unresolvedName)
            this.source = sourceForTypeRef
        }
    }

    /**
     * This is required to avoid contract violation during [org.jetbrains.kotlin.fir.declarations.getOwnDeprecationForCallSite]
     * Because argument transformation may lead to [org.jetbrains.kotlin.fir.declarations.FirResolvePhase.TYPES]+ lazy resolution
     * See KT-59342
     * TODO: KT-60520
     */
    val argumentMapping = object : FirAnnotationArgumentMapping() {
        override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}
        override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement = this
        override val source: KtSourceElement? get() = null

        override val mapping: Map<Name, FirExpression> by lazy {
            when {
                classId == JvmStandardClassIds.Annotations.Java.Target -> {
                    when (val argument = javaAnnotation.arguments.firstOrNull()) {
                        is JavaArrayAnnotationArgument -> argument.getElements().mapJavaTargetArguments()
                        is JavaEnumValueAnnotationArgument -> listOf(argument).mapJavaTargetArguments()
                        else -> null
                    }?.let {
                        mapOf(StandardClassIds.Annotations.ParameterNames.targetAllowedTargets to it)
                    }
                }

                classId == JvmStandardClassIds.Annotations.Java.Retention -> {
                    javaAnnotation.arguments.firstOrNull()?.mapJavaRetentionArgument()?.let {
                        mapOf(StandardClassIds.Annotations.ParameterNames.retentionValue to it)
                    }
                }

                classId == JvmStandardClassIds.Annotations.Java.Deprecated -> {
                    mapOf(
                        StandardClassIds.Annotations.ParameterNames.deprecatedMessage to "Deprecated in Java".createConstantOrError(
                            session,
                        )
                    )
                }

                lookupTag == null -> null
                else -> javaAnnotation.arguments.ifNotEmpty {
                    val mapping = LinkedHashMap<Name, FirExpression>(size)
                    fillAnnotationArgumentMapping(session, lookupTag, this, mapping, source)
                    mapping
                }
            }.orEmpty()
        }
    }

    return AnnotationData(annotationTypeRef, argumentMapping)
}
