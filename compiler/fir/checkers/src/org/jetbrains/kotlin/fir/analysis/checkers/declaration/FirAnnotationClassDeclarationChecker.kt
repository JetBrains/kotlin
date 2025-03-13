/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes.VALUE_PARAMETER
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.hasValOrVar
import org.jetbrains.kotlin.diagnostics.hasVar
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getTargetAnnotation
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CYCLE_IN_ANNOTATION_PARAMETER
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.canBeEvaluatedAtCompileTime
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.primitiveArrayTypeByElementType
import org.jetbrains.kotlin.name.StandardClassIds.unsignedArrayTypeByElementType
import org.jetbrains.kotlin.types.Variance

object FirAnnotationClassDeclarationChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ANNOTATION_CLASS) return
        if (declaration.isLocal) reporter.reportOn(declaration.source, FirErrors.LOCAL_ANNOTATION_CLASS_ERROR, context)

        if (declaration.superTypeRefs.size != 1) {
            reporter.reportOn(declaration.source, FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS, context)
        }

        declaration.processAllDeclarations(context.session) { member ->
            checkAnnotationClassMember(member, context, reporter)
        }

        val session = context.session
        if (declaration.getRetention(session) != AnnotationRetention.SOURCE &&
            KotlinTarget.EXPRESSION in declaration.getAllowedAnnotationTargets(session)
        ) {
            val target = declaration.getRetentionAnnotation(session) ?: declaration.getTargetAnnotation(session) ?: declaration
            reporter.reportOn(target.source, FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION, context)
        }

        checkCyclesInParameters(declaration.symbol, context, reporter)
    }

    private fun checkAnnotationClassMember(member: FirBasedSymbol<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        when {
            member is FirConstructorSymbol && member.isPrimary -> {
                for (parameter in member.valueParameterSymbols) {
                    val source = parameter.source ?: continue
                    if (!source.hasValOrVar()) {
                        reporter.reportOn(source, FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER, context)
                    } else if (source.hasVar()) {
                        reporter.reportOn(source, FirErrors.VAR_ANNOTATION_PARAMETER, context)
                    }
                    if (parameter.hasDefaultValue && !canBeEvaluatedAtCompileTime(
                            parameter.resolvedDefaultValue, context.session, allowErrors = true, calledOnCheckerStage = true
                        )
                    ) {
                        reporter.reportOn(
                            parameter.defaultValueSource, FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, context
                        )
                    }

                    val typeRef = parameter.resolvedReturnTypeRef
                    val coneType = typeRef.coneType.fullyExpandedType(context.session)
                    val classId = coneType.classId

                    when {
                        coneType is ConeErrorType -> {
                            // DO NOTHING: error types already have diagnostics which are reported elsewhere.
                        }
                        coneType.isMarkedNullable -> {
                            reporter.reportOn(typeRef.source, FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER, context)
                        }
                        coneType.isPrimitiveOrNullablePrimitive -> {
                            // DO NOTHING: primitives are allowed as annotation class parameter
                        }
                        coneType.isUnsignedTypeOrNullableUnsignedType -> {
                            // DO NOTHING: unsigned types are allowed as annotation class parameter.
                        }
                        classId == StandardClassIds.KClass -> {
                            // DO NOTHING: KClass is allowed
                        }
                        classId == StandardClassIds.String -> {
                            // DO NOTHING: String is allowed
                        }
                        classId in primitiveArrayTypeByElementType.values -> {
                            // DO NOTHING: primitive arrays are allowed
                        }
                        classId in unsignedArrayTypeByElementType.values -> {
                            // DO NOTHING: arrays of unsigned types are allowed
                        }
                        classId == StandardClassIds.Array -> {
                            if (!isAllowedArray(coneType, context.session)) {
                                reporter.reportOn(typeRef.source, FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER, context)
                            } else if (!parameter.isVararg && coneType.typeArguments.firstOrNull()?.variance != Variance.INVARIANT) {
                                reporter.reportOn(typeRef.source, FirErrors.PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER, context)
                            }
                        }
                        isAllowedClassKind(coneType, context.session) -> {
                            // DO NOTHING: annotation or enum classes are allowed
                        }
                        else -> {
                            reporter.reportOn(typeRef.source, FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER, context)
                        }
                    }
                }
            }
            member is FirRegularClassSymbol -> {
                // DO NOTHING: nested annotation classes are allowed in 1.3+
            }
            member is FirPropertySymbol && member.source?.elementType == VALUE_PARAMETER -> {
                // DO NOTHING to avoid reporting constructor properties
            }
            member is FirNamedFunctionSymbol && member.isSynthetic -> {
                // DO NOTHING to avoid reporting synthetic functions
            }
            else -> {
                reporter.reportOn(member.source, FirErrors.ANNOTATION_CLASS_MEMBER, context)
            }
        }
    }

    private fun isAllowedClassKind(cone: ConeKotlinType, session: FirSession): Boolean {
        val typeRefClassKind = cone.toRegularClassSymbol(session)
            ?.classKind
            ?: return false

        return typeRefClassKind == ANNOTATION_CLASS || typeRefClassKind == ENUM_CLASS
    }

    private fun isAllowedArray(type: ConeKotlinType, session: FirSession): Boolean {
        val typeArguments = type.typeArguments

        if (typeArguments.size != 1) return false

        val arrayType = (typeArguments[0] as? ConeKotlinTypeProjection)?.type?.fullyExpandedType(session)
            ?: return false

        if (arrayType.isMarkedNullable) return false

        val arrayTypeClassId = arrayType.classId

        when {
            arrayTypeClassId == StandardClassIds.KClass -> {
                // KClass is allowed
                return true
            }
            arrayTypeClassId == StandardClassIds.String -> {
                // String is allowed
                return true
            }
            isAllowedClassKind(arrayType as ConeLookupTagBasedType, session) -> {
                // annotation or enum classes are allowed
                return true
            }
        }

        return false
    }

    private fun checkCyclesInParameters(annotation: FirRegularClassSymbol, context: CheckerContext, reporter: DiagnosticReporter) {
        val primaryConstructor = annotation.primaryConstructorIfAny(context.session) ?: return
        val checker = CycleChecker(annotation, context.session)
        for (valueParameter in primaryConstructor.valueParameterSymbols) {
            if (checker.parameterHasCycle(annotation, valueParameter)) {
                reporter.reportOn(valueParameter.source, CYCLE_IN_ANNOTATION_PARAMETER, context)
            }
        }
    }

    private class CycleChecker(val targetAnnotation: FirRegularClassSymbol, val session: FirSession) {
        private val visitedAnnotations = mutableSetOf(targetAnnotation)
        private val annotationsWithCycle = mutableSetOf(targetAnnotation)

        fun annotationHasCycle(annotation: FirRegularClassSymbol): Boolean {
            val primaryConstructor = annotation.primaryConstructorIfAny(session) ?: return false
            for (valueParameter in primaryConstructor.valueParameterSymbols) {
                if (parameterHasCycle(annotation, valueParameter)) return true
            }
            return false
        }

        fun parameterHasCycle(ownedAnnotation: FirRegularClassSymbol, parameter: FirValueParameterSymbol): Boolean {
            val returnType = parameter.resolvedReturnTypeRef.coneType.fullyExpandedType(session)
            return when {
                parameter.isVararg || returnType.isNonPrimitiveArray -> false
                returnType.typeArguments.isNotEmpty() -> {
                    if (returnType.classId == StandardClassIds.KClass) return false
                    for (argument in returnType.typeArguments) {
                        if (typeHasCycle(ownedAnnotation, argument.type ?: continue)) return true
                    }
                    false
                }
                else -> typeHasCycle(ownedAnnotation, returnType)
            }
        }

        fun typeHasCycle(ownedAnnotation: FirRegularClassSymbol, type: ConeKotlinType): Boolean {
            val referencedAnnotation = type.fullyExpandedType(session)
                .toRegularClassSymbol(session)
                ?.takeIf { it.classKind == ANNOTATION_CLASS }
                ?: return false
            if (!visitedAnnotations.add(referencedAnnotation)) {
                return (referencedAnnotation in annotationsWithCycle).also {
                    if (it) {
                        annotationsWithCycle += ownedAnnotation
                    }
                }
            }
            if (referencedAnnotation == targetAnnotation) {
                annotationsWithCycle += ownedAnnotation
                return true
            }
            if (referencedAnnotation.isJavaOrEnhancement) {
                return false
            }
            return annotationHasCycle(referencedAnnotation)
        }
    }
}
