/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.util.getChildren

object FirSupertypesChecker : FirClassChecker(MppCheckerKind.Platform) {
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
            if (superTypeRef.source == null || superTypeRef.source?.kind == KtFakeSourceElementKind.EnumSuperTypeRef) continue

            val expandedSupertype = superTypeRef.coneType.fullyExpandedType(context.session)
            val originalSupertype = expandedSupertype.abbreviatedTypeOrSelf
            val supertypeIsDynamic = originalSupertype is ConeDynamicType
            if (!nullableSupertypeReported && originalSupertype.isMarkedNullable) {
                reporter.reportOn(superTypeRef.source, FirErrors.NULLABLE_SUPERTYPE, context)
                nullableSupertypeReported = true
            }
            if (!extensionFunctionSupertypeReported && originalSupertype.isExtensionFunctionType &&
                !context.session.languageVersionSettings.supportsFeature(LanguageFeature.FunctionalTypeWithExtensionAsSupertype)
            ) {
                reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE, context)
                extensionFunctionSupertypeReported = true
            }

            checkAnnotationOnSuperclass(superTypeRef, context, reporter)

            val symbol = expandedSupertype.toSymbol(context.session)

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
                    // DYNAMIC_SUPERTYPE will be reported separately
                    if (!interfaceWithSuperclassReported && !supertypeIsDynamic) {
                        reporter.reportOn(superTypeRef.source, FirErrors.INTERFACE_WITH_SUPERCLASS, context)
                        interfaceWithSuperclassReported = true
                    }
                }
                val isObject = symbol.classKind == ClassKind.OBJECT
                // DYNAMIC_SUPERTYPE will be reported separately
                if (!finalSupertypeReported && !isObject && symbol.modality == Modality.FINAL && !supertypeIsDynamic) {
                    reporter.reportOn(superTypeRef.source, FirErrors.FINAL_SUPERTYPE, context)
                    finalSupertypeReported = true
                }
                if (!singletonInSupertypeReported && isObject) {
                    reporter.reportOn(superTypeRef.source, FirErrors.SINGLETON_IN_SUPERTYPE, context)
                    singletonInSupertypeReported = true
                }
            }

            checkClassCannotBeExtendedDirectly(symbol, reporter, superTypeRef, context)
            checkNamedFunctionTypeParameter(superTypeRef, context, reporter)

            val shouldCheckSupertypeOnTypealiasWithTypeProjection = if (originalSupertype.typeArguments.isNotEmpty()) {
                !checkProjectionInImmediateArgumentToSupertype(originalSupertype, superTypeRef, reporter, context)
            } else {
                !checkExpandedTypeCannotBeInherited(symbol, expandedSupertype, reporter, superTypeRef, originalSupertype, context)
            }

            if (shouldCheckSupertypeOnTypealiasWithTypeProjection) {
                checkSupertypeOnTypeAliasWithTypeProjection(originalSupertype, expandedSupertype, superTypeRef, reporter, context)
            }
        }

        checkDelegationNotToInterface(declaration, context, reporter)
        checkDelegationWithoutPrimaryConstructor(declaration, context, reporter)

        if (declaration is FirRegularClass && declaration.superTypeRefs.size > 1) {
            checkInconsistentTypeParameters(listOf(null to declaration.symbol), context, reporter, declaration.source, isValues = true)
        }
    }

    private fun checkAnnotationOnSuperclass(
        superTypeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
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
        context: CheckerContext,
    ) {
        if (symbol is FirRegularClassSymbol && symbol.classId == StandardClassIds.Enum) {
            reporter.reportOn(superTypeRef.source, FirErrors.CLASS_CANNOT_BE_EXTENDED_DIRECTLY, symbol, context)
        }
    }

    private fun checkProjectionInImmediateArgumentToSupertype(
        coneType: ConeKotlinType,
        superTypeRef: FirTypeRef,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ): Boolean {
        val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(superTypeRef) ?: return false
        var result = false
        for ((index, typeArgument) in coneType.typeArguments.withIndex()) {
            if (typeArgument.isConflictingOrNotInvariant) {
                val (_, argSource) = typeRefAndSourcesForArguments.getOrNull(index) ?: continue
                reporter.reportOn(
                    argSource ?: superTypeRef.source,
                    FirErrors.PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE,
                    context
                )
                result = true
            }
        }
        return result
    }

    private fun checkExpandedTypeCannotBeInherited(
        symbol: FirBasedSymbol<*>?,
        fullyExpandedType: ConeKotlinType,
        reporter: DiagnosticReporter,
        superTypeRef: FirTypeRef,
        coneType: ConeKotlinType,
        context: CheckerContext,
    ): Boolean {
        if (symbol is FirRegularClassSymbol && symbol.classKind == ClassKind.INTERFACE) {
            for (typeArgument in fullyExpandedType.typeArguments) {
                if (typeArgument.isConflictingOrNotInvariant) {
                    reporter.reportOn(superTypeRef.source, FirErrors.EXPANDED_TYPE_CANNOT_BE_INHERITED, coneType, context)
                    return true
                }
            }
        }
        return false
    }

    private fun checkSupertypeOnTypeAliasWithTypeProjection(
        coneType: ConeKotlinType,
        fullyExpandedType: ConeKotlinType,
        superTypeRef: FirTypeRef,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (coneType.toSymbol(context.session) is FirTypeAliasSymbol &&
            fullyExpandedType.typeArguments.any { it.isConflictingOrNotInvariant }
        ) {
            reporter.reportOn(superTypeRef.source, FirErrors.CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION, context)
        }
    }

    private fun checkDelegationNotToInterface(
        declaration: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        @OptIn(DirectDeclarationsAccess::class)
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

    private fun checkNamedFunctionTypeParameter(
        superTypeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val delegatedTypeRef = (superTypeRef as? FirResolvedTypeRef)?.delegatedTypeRef ?: return
        if (delegatedTypeRef !is FirFunctionTypeRef) return
        for (parameter in delegatedTypeRef.parameters) {
            if (parameter.name != null) {
                val source = parameter.findSourceForParameterName() ?: continue
                reporter.reportOn(
                    source,
                    FirErrors.UNSUPPORTED,
                    "Named parameter in function type as supertype is unsupported.",
                    context
                )
            }
        }
    }

    private fun FirFunctionTypeParameter.findSourceForParameterName(): KtSourceElement? {
        val name = this.name ?: return null
        val treeStructure = source.treeStructure
        val nodes = source.lighterASTNode.getChildren(treeStructure)
        val node = nodes.find { it.tokenType == KtTokens.IDENTIFIER && treeStructure.toString(it) == name.identifier } ?: return null

        return node.toKtLightSourceElement(
            treeStructure,
            startOffset = node.startOffset,
            endOffset = node.endOffset
        )
    }


    private fun checkDelegationWithoutPrimaryConstructor(
        declaration: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (declaration.isInterface) return
        if (declaration.isExpect) return
        val primaryConstructor = declaration.primaryConstructorIfAny(context.session)
        if (primaryConstructor != null) return
        @OptIn(DirectDeclarationsAccess::class)
        for (subDeclaration in declaration.declarations) {
            if (subDeclaration !is FirField) continue
            if (subDeclaration.symbol.visibility == Visibilities.Private && subDeclaration.name.isDelegated) {
                reporter.reportOn(
                    subDeclaration.source,
                    FirErrors.UNSUPPORTED,
                    "Delegation without primary constructor is unsupported.",
                    context
                )
            }
        }
    }

}
