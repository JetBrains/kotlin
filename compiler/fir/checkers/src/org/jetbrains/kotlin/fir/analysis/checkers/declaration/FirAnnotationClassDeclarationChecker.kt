/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.StandardClassIds.primitiveArrayTypeByElementType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds.primitiveTypes
import org.jetbrains.kotlin.fir.symbols.StandardClassIds.unsignedTypes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.KtNodeTypes.FUN
import org.jetbrains.kotlin.KtNodeTypes.VALUE_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.name.ClassId

object FirAnnotationClassDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) return
        if (declaration.classKind != ANNOTATION_CLASS) return
        if (declaration.isLocal) reporter.report(declaration.source, FirErrors.LOCAL_ANNOTATION_CLASS_ERROR)

        for (it in declaration.declarations) {
            when {
                it is FirConstructor && it.isPrimary -> {
                    for (parameter in it.valueParameters) {
                        val source = parameter.source ?: continue
                        if (!source.hasValOrVar()) {
                            reporter.report(source, FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER)
                        } else if (source.hasVar()) {
                            reporter.report(source, FirErrors.VAR_ANNOTATION_PARAMETER)
                        }

                        val typeRef = parameter.returnTypeRef
                        val coneType = typeRef.coneTypeSafe<ConeLookupTagBasedType>()
                        val classId = coneType?.classId

                        if (coneType != null) when {
                            classId == ClassId.fromString("<error>") -> {
                                // TODO: replace with UNRESOLVED_REFERENCE check
                            }
                            coneType.isNullable -> {
                                reporter.report(typeRef.source, FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER)
                            }
                            classId in primitiveTypes -> {
                                // DO NOTHING: primitives are allowed as annotation class parameter
                            }
                            classId in unsignedTypes -> {
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
                                    reporter.report(typeRef.source, FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER)
                            }
                            isAllowedClassKind(coneType, context.session) -> {
                                // DO NOTHING: annotation or enum classes are allowed
                            }
                            else -> {
                                reporter.report(typeRef.source, FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER)
                            }
                        }
                    }
                }
                it is FirRegularClass -> {
                    // DO NOTHING: nested annotation classes are allowed in 1.3+
                }
                it is FirProperty && it.source?.elementType == VALUE_PARAMETER -> {
                    // DO NOTHING to avoid reporting constructor properties
                }
                it is FirSimpleFunction && it.source?.elementType != FUN -> {
                    // DO NOTHING to avoid reporting synthetic functions
                    // TODO: replace with origin check
                }
                else -> {
                    reporter.report(it.source, FirErrors.ANNOTATION_CLASS_MEMBER)
                }
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

    private inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.report(
        source: T?,
        factory: FirDiagnosticFactory0<T, P>
    ) {
        source?.let { report(factory.on(it)) }
    }
}
