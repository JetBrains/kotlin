/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes.FUN
import org.jetbrains.kotlin.KtNodeTypes.VALUE_PARAMETER
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.checkConstantArguments
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.primitiveArrayTypeByElementType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

object FirAnnotationClassDeclarationChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ANNOTATION_CLASS) return
        if (declaration.isLocal) reporter.reportOn(declaration.source, FirErrors.LOCAL_ANNOTATION_CLASS_ERROR, context)

        if (declaration.superTypeRefs.size != 1) {
            reporter.reportOn(declaration.source, FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS, context)
        }

        for (member in declaration.declarations) {
            checkAnnotationClassMember(member, context, reporter)
        }

        if (declaration.getRetention() != AnnotationRetention.SOURCE &&
            KotlinTarget.EXPRESSION in declaration.getAllowedAnnotationTargets()
        ) {
            val target = declaration.getRetentionAnnotation() ?: declaration.getTargetAnnotation() ?: declaration
            reporter.reportOn(target.source, FirErrors.RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION, context)
        }
    }

    private fun checkAnnotationClassMember(member: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when {
            member is FirConstructor && member.isPrimary -> {
                for (parameter in member.valueParameters) {
                    val source = parameter.source ?: continue
                    if (!source.hasValOrVar()) {
                        reporter.reportOn(source, FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER, context)
                    } else if (source.hasVar()) {
                        reporter.reportOn(source, FirErrors.VAR_ANNOTATION_PARAMETER, context)
                    }
                    val defaultValue = parameter.defaultValue
                    if (defaultValue != null && checkConstantArguments(defaultValue, context.session) != null) {
                        reporter.reportOn(defaultValue.source, FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT, context)
                    }

                    val typeRef = parameter.returnTypeRef
                    val coneType = typeRef.coneTypeSafe<ConeLookupTagBasedType>()
                    val classId = coneType?.classId

                    if (coneType != null) when {
                        classId == ClassId.fromString("<error>") -> {
                            // TODO: replace with UNRESOLVED_REFERENCE check
                        }
                        coneType.isNullable -> {
                            reporter.reportOn(typeRef.source, FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER, context)
                        }
                        coneType.isPrimitiveOrNullablePrimitive -> {
                            // DO NOTHING: primitives are allowed as annotation class parameter
                        }
                        coneType.isUnsignedTypeOrNullableUnsignedType -> {
                            // TODO: replace with EXPERIMENTAL_UNSIGNED_LITERALS check
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
                        classId == StandardClassIds.Array -> {
                            if (!isAllowedArray(typeRef, context.session))
                                reporter.reportOn(typeRef.source, FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER, context)
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
            member is FirRegularClass -> {
                // DO NOTHING: nested annotation classes are allowed in 1.3+
            }
            member is FirProperty && member.source?.elementType == VALUE_PARAMETER -> {
                // DO NOTHING to avoid reporting constructor properties
            }
            member is FirSimpleFunction && member.source?.elementType != FUN -> {
                // DO NOTHING to avoid reporting synthetic functions
                // TODO: replace with origin check
            }
            else -> {
                reporter.reportOn(member.source, FirErrors.ANNOTATION_CLASS_MEMBER, context)
            }
        }
    }

    private fun isAllowedClassKind(cone: ConeLookupTagBasedType, session: FirSession): Boolean {
        val typeRefClassKind = (cone.lookupTag.toSymbol(session)
            ?.fir as? FirRegularClass)
            ?.classKind
            ?: return false

        return typeRefClassKind == ANNOTATION_CLASS || typeRefClassKind == ENUM_CLASS
    }

    private fun isAllowedArray(typeRef: FirTypeRef, session: FirSession): Boolean {
        val typeArguments = typeRef.coneType.typeArguments

        if (typeArguments.size != 1) return false

        val arrayType = (typeArguments[0] as? ConeKotlinTypeProjection)
            ?.type
            ?: return false

        if (arrayType.isNullable) return false

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
}
