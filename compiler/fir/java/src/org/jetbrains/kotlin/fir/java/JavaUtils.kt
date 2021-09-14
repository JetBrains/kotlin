/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.bindSymbolToLookupTag
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.expectedConeType
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
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

private fun List<JavaAnnotationArgument>.mapJavaTargetArguments(session: FirSession): FirExpression? {
    return buildVarargArgumentsExpression {
        val resultSet = EnumSet.noneOf(AnnotationTarget::class.java)
        for (target in this@mapJavaTargetArguments) {
            if (target !is JavaEnumValueAnnotationArgument) return null
            resultSet.addAll(JAVA_TARGETS_TO_KOTLIN[target.entryName?.asString()] ?: continue)
        }
        val classId = StandardClassIds.AnnotationTarget
        resultSet.mapTo(arguments) { buildEnumCall(session, classId, Name.identifier(it.name)) }
        varargElementType = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(classId),
                emptyArray(),
                isNullable = false,
                ConeAttributes.Empty
            ).createOutArrayType()
        }
    }
}

private val JAVA_RETENTION_TO_KOTLIN = mapOf(
    "RUNTIME" to AnnotationRetention.RUNTIME,
    "CLASS" to AnnotationRetention.BINARY,
    "SOURCE" to AnnotationRetention.SOURCE
)

private fun JavaAnnotationArgument.mapJavaRetentionArgument(session: FirSession): FirExpression? =
    JAVA_RETENTION_TO_KOTLIN[(this as? JavaEnumValueAnnotationArgument)?.entryName?.asString()]?.let {
        buildEnumCall(session, StandardClassIds.AnnotationRetention, Name.identifier(it.name))
    }

private fun fillAnnotationArgumentMapping(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    lookupTag: ConeClassLikeLookupTagImpl,
    annotationArguments: Collection<JavaAnnotationArgument>,
    destination: MutableMap<Name, FirExpression>
) {
    if (annotationArguments.isEmpty()) return

    val annotationClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(lookupTag.classId).also {
        lookupTag.bindSymbolToLookupTag(session, it)
    }
    val annotationConstructor = (annotationClassSymbol?.fir as FirRegularClass?)
        ?.declarations
        ?.firstIsInstanceOrNull<FirConstructor>()
    annotationArguments.associateTo(destination) { argument ->
        val name = argument.name ?: ParameterNames.value
        val parameter = annotationConstructor?.valueParameters?.find { it.name == name }
        name to argument.toFirExpression(session, javaTypeParameterStack, parameter?.returnTypeRef)
    }
}

internal fun Iterable<JavaAnnotation>.convertAnnotationsToFir(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): List<FirAnnotation> = map { it.toFirAnnotationCall(session, javaTypeParameterStack) }

internal fun JavaAnnotationOwner.convertAnnotationsToFir(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): List<FirAnnotation> = annotations.convertAnnotationsToFir(session, javaTypeParameterStack)

internal fun MutableList<FirAnnotation>.addFromJava(
    session: FirSession,
    javaAnnotationOwner: JavaAnnotationOwner,
    javaTypeParameterStack: JavaTypeParameterStack
) {
    javaAnnotationOwner.annotations.mapTo(this) { it.toFirAnnotationCall(session, javaTypeParameterStack) }
}

private fun JavaAnnotation.toFirAnnotationCall(
    session: FirSession, javaTypeParameterStack: JavaTypeParameterStack
): FirAnnotation {
    return buildAnnotation {
        val lookupTag = when (classId) {
            Annotations.Java.Target -> Annotations.Target
            Annotations.Java.Retention -> Annotations.Retention
            Annotations.Java.Documented -> Annotations.MustBeDocumented
            Annotations.Java.Deprecated -> Annotations.Deprecated
            else -> classId
        }?.let(::ConeClassLikeLookupTagImpl)
        annotationTypeRef = if (lookupTag != null) {
            buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(lookupTag, emptyArray(), isNullable = false)
            }
        } else {
            buildErrorTypeRef { diagnostic = ConeUnresolvedReferenceError() }
        }

        argumentMapping = buildAnnotationArgumentMapping {
            when (classId) {
                Annotations.Java.Target -> {
                    when (val argument = arguments.firstOrNull()) {
                        is JavaArrayAnnotationArgument -> argument.getElements().mapJavaTargetArguments(session)
                        is JavaEnumValueAnnotationArgument -> listOf(argument).mapJavaTargetArguments(session)
                        else -> null
                    }?.let {
                        mapping[ParameterNames.targetAllowedTargets] = it
                    }
                }
                Annotations.Java.Retention -> {
                    arguments.firstOrNull()?.mapJavaRetentionArgument(session)?.let {
                        mapping[ParameterNames.retentionValue] = it
                    }
                }
                Annotations.Java.Deprecated -> {
                    mapping[ParameterNames.deprecatedMessage] = buildConstExpression(
                        source = null,
                        ConstantValueKind.String,
                        "Deprecated in Java"
                    ).setProperType(session)
                }
                else -> {
                    fillAnnotationArgumentMapping(session, javaTypeParameterStack, lookupTag!!, arguments, mapping)
                }
            }
        }
    }
}

internal fun JavaValueParameter.toFirValueParameter(
    session: FirSession, moduleData: FirModuleData, index: Int, javaTypeParameterStack: JavaTypeParameterStack
): FirValueParameter {
    return buildJavaValueParameter {
        source = (this@toFirValueParameter as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
        this.moduleData = moduleData
        name = this@toFirValueParameter.name ?: Name.identifier("p$index")
        returnTypeRef = type.toFirJavaTypeRef(session, javaTypeParameterStack)
        isVararg = this@toFirValueParameter.isVararg
        annotationBuilder = { convertAnnotationsToFir(session, javaTypeParameterStack) }
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
