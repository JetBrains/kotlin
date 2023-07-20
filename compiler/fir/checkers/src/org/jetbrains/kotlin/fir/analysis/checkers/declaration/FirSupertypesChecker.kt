/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

object FirSupertypesChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        var nullableSupertypeReported = false
        var extensionFunctionSupertypeReported = false
        var interfaceWithSuperclassReported = !isInterface
        var finalSupertypeReported = false
        var singletonInSupertypeReported = false
        var classAppeared = false
        val superClassSymbols = hashSetOf<FirRegularClassSymbol>()
        for (superTypeRef in declaration.superTypeRefs) {
            // skip implicit super types like Enum or Any
            if (superTypeRef.source == null) continue

            val coneType = superTypeRef.coneType
            if (!nullableSupertypeReported && coneType.nullability == ConeNullability.NULLABLE) {
                reporter.reportOn(superTypeRef.source, FirErrors.NULLABLE_SUPERTYPE, context)
                nullableSupertypeReported = true
            }
            if (!extensionFunctionSupertypeReported && coneType.isExtensionFunctionType &&
                !context.session.languageVersionSettings.supportsFeature(LanguageFeature.FunctionalTypeWithExtensionAsSupertype)
            ) {
                reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE, context)
                extensionFunctionSupertypeReported = true
            }

            checkAnnotationOnSuperclass(superTypeRef, context, reporter)

            val fullyExpandedType = coneType.fullyExpandedType(context.session)
            val symbol = fullyExpandedType.toSymbol(context.session)

            if (symbol is FirRegularClassSymbol) {
                if (!superClassSymbols.add(symbol)) {
                    reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_APPEARS_TWICE, context)
                }
                if (symbol.classKind != ClassKind.INTERFACE) {
                    if (classAppeared) {
                        reporter.reportOn(superTypeRef.source, FirErrors.MANY_CLASSES_IN_SUPERTYPE_LIST, context)
                    } else {
                        classAppeared = true
                    }
                    if (!interfaceWithSuperclassReported) {
                        reporter.reportOn(superTypeRef.source, FirErrors.INTERFACE_WITH_SUPERCLASS, context)
                        interfaceWithSuperclassReported = true
                    }
                }
                val isObject = symbol.classKind == ClassKind.OBJECT
                if (!finalSupertypeReported && !isObject && symbol.modality == Modality.FINAL) {
                    reporter.reportOn(superTypeRef.source, FirErrors.FINAL_SUPERTYPE, context)
                    finalSupertypeReported = true
                }
                if (!singletonInSupertypeReported && isObject) {
                    reporter.reportOn(superTypeRef.source, FirErrors.SINGLETON_IN_SUPERTYPE, context)
                    singletonInSupertypeReported = true
                }
            }

            checkClassCannotBeExtendedDirectly(symbol, reporter, superTypeRef, context)

            if (coneType.typeArguments.isNotEmpty()) {
                checkProjectionInImmediateArgumentToSupertype(coneType, superTypeRef, reporter, context)
            } else {
                checkExpandedTypeCannotBeInherited(symbol, fullyExpandedType, reporter, superTypeRef, coneType, context)
            }
        }

        checkDelegationNotToInterface(declaration, context, reporter)

        if (declaration is FirRegularClass && declaration.superTypeRefs.size > 1) {
            checkInconsistentTypeParameters(listOf(Pair(null, declaration.symbol)), context, reporter, declaration.source, true)
        }
    }

    private fun checkAnnotationOnSuperclass(
        superTypeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (annotation in superTypeRef.annotations) {
            if (annotation.useSiteTarget != null) {
                reporter.reportOn(annotation.source, FirErrors.ANNOTATION_ON_SUPERCLASS, context)
            }
        }
    }

    private fun checkClassCannotBeExtendedDirectly(
        symbol: FirClassifierSymbol<*>?,
        reporter: DiagnosticReporter,
        superTypeRef: FirTypeRef,
        context: CheckerContext
    ) {
        if (symbol is FirRegularClassSymbol && symbol.classId == StandardClassIds.Enum) {
            reporter.reportOn(superTypeRef.source, FirErrors.CLASS_CANNOT_BE_EXTENDED_DIRECTLY, symbol, context)
        }
    }

    private fun checkProjectionInImmediateArgumentToSupertype(
        coneType: ConeKotlinType,
        superTypeRef: FirTypeRef,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(superTypeRef) ?: return
        for ((index, typeArgument) in coneType.typeArguments.withIndex()) {
            if (typeArgument.isConflictingOrNotInvariant) {
                val (_, argSource) = typeRefAndSourcesForArguments.getOrNull(index) ?: continue
                reporter.reportOn(
                    argSource ?: superTypeRef.source,
                    FirErrors.PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE,
                    context
                )
            }
        }
    }

    private fun checkExpandedTypeCannotBeInherited(
        symbol: FirBasedSymbol<*>?,
        fullyExpandedType: ConeKotlinType,
        reporter: DiagnosticReporter,
        superTypeRef: FirTypeRef,
        coneType: ConeKotlinType,
        context: CheckerContext
    ) {
        if (symbol is FirRegularClassSymbol && symbol.classKind == ClassKind.INTERFACE) {
            for (typeArgument in fullyExpandedType.typeArguments) {
                if (typeArgument.isConflictingOrNotInvariant) {
                    reporter.reportOn(superTypeRef.source, FirErrors.EXPANDED_TYPE_CANNOT_BE_INHERITED, coneType, context)
                    break
                }
            }
        }
    }

    private fun checkDelegationNotToInterface(
        declaration: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (subDeclaration in declaration.declarations) {
            if (subDeclaration is FirField) {
                if (subDeclaration.visibility == Visibilities.Private && subDeclaration.name.isDelegated) {
                    val delegatedClassSymbol = subDeclaration.returnTypeRef.toRegularClassSymbol(context.session)
                    if (delegatedClassSymbol != null && delegatedClassSymbol.classKind != ClassKind.INTERFACE) {
                        reporter.reportOn(subDeclaration.returnTypeRef.source, FirErrors.DELEGATION_NOT_TO_INTERFACE, context)
                    }
                }
            }
        }
    }
}
