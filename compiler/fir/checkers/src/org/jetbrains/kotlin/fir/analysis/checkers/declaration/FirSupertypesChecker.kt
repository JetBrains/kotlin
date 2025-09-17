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
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.languageVersionSettings
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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        var extensionOrContextFunctionSupertypeReported = false
        var interfaceWithSuperclassReported = !isInterface
        var finalSupertypeReported = false
        var singletonInSupertypeReported = false
        var classAppeared = false
        val superClassSymbols = hashSetOf<FirRegularClassSymbol>()
        for (superTypeRef in declaration.superTypeRefs) {
            // skip implicit super types like Enum or Any
            if (superTypeRef.source == null || superTypeRef.source?.kind == KtFakeSourceElementKind.EnumSuperTypeRef) continue

            val expandedSupertype = superTypeRef.coneType.fullyExpandedType()
            val originalSupertype = expandedSupertype.abbreviatedTypeOrSelf
            val supertypeIsDynamic = originalSupertype is ConeDynamicType
            when {
                originalSupertype.isMarkedNullable -> {
                    reporter.reportOn(superTypeRef.source, FirErrors.NULLABLE_SUPERTYPE)
                }
                expandedSupertype.isMarkedNullable -> {
                    reporter.reportOn(superTypeRef.source, FirErrors.NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS)
                }
            }
            if (!extensionOrContextFunctionSupertypeReported &&
                originalSupertype.fullyExpandedType().let { it.isExtensionFunctionType || it.hasContextParameters } &&
                !LanguageFeature.FunctionalTypeWithExtensionAsSupertype.isEnabled()
            ) {
                reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE)
                extensionOrContextFunctionSupertypeReported = true
            }

            checkAnnotationOnSuperclass(superTypeRef)

            val symbol = expandedSupertype.toSymbol()
            val allowUsingClassTypeAsInterface =
                context.session.languageVersionSettings.supportsFeature(LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface) &&
                        expandedSupertype.isAny &&
                        expandedSupertype.abbreviatedType != null

            if (symbol is FirRegularClassSymbol) {
                if (!superClassSymbols.add(symbol)) {
                    reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_APPEARS_TWICE)
                }
                if (symbol.classKind != ClassKind.INTERFACE) {
                    if (classAppeared) {
                        if (!allowUsingClassTypeAsInterface) {
                            reporter.reportOn(superTypeRef.source, FirErrors.MANY_CLASSES_IN_SUPERTYPE_LIST)
                        }
                    } else {
                        classAppeared = true
                    }
                    // DYNAMIC_SUPERTYPE will be reported separately
                    if (!interfaceWithSuperclassReported && !supertypeIsDynamic) {
                        if (!allowUsingClassTypeAsInterface) {
                            reporter.reportOn(superTypeRef.source, FirErrors.INTERFACE_WITH_SUPERCLASS)
                            interfaceWithSuperclassReported = true
                        }
                    }
                }
                val isObject = symbol.classKind == ClassKind.OBJECT
                // DYNAMIC_SUPERTYPE will be reported separately
                if (!finalSupertypeReported && !isObject && symbol.modality == Modality.FINAL && !supertypeIsDynamic) {
                    reporter.reportOn(superTypeRef.source, FirErrors.FINAL_SUPERTYPE)
                    finalSupertypeReported = true
                }
                if (!singletonInSupertypeReported && isObject) {
                    reporter.reportOn(superTypeRef.source, FirErrors.SINGLETON_IN_SUPERTYPE)
                    singletonInSupertypeReported = true
                }
            }

            checkClassCannotBeExtendedDirectly(symbol, superTypeRef)
            checkNamedFunctionTypeParameter(superTypeRef)

            if (!checkProjectionInImmediateArgumentToSupertype(originalSupertype, superTypeRef)) {
                checkSupertypeOnTypeAliasWithTypeProjection(originalSupertype, expandedSupertype, superTypeRef)
            }
        }

        checkDelegationNotToInterface(declaration)
        checkDelegationWithoutPrimaryConstructor(declaration)

        if (declaration.superTypeRefs.size > 1) {
            checkInconsistentTypeParameters(listOf(null to declaration.symbol), declaration.source, isValues = true)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationOnSuperclass(
        superTypeRef: FirTypeRef,
    ) {
        for (annotation in superTypeRef.annotations) {
            if (annotation.useSiteTarget != null) {
                reporter.reportOn(annotation.source, FirErrors.ANNOTATION_ON_SUPERCLASS_ERROR)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkClassCannotBeExtendedDirectly(
        symbol: FirClassifierSymbol<*>?,
        superTypeRef: FirTypeRef,
    ) {
        if (symbol is FirRegularClassSymbol && symbol.classId == StandardClassIds.Enum) {
            reporter.reportOn(superTypeRef.source, FirErrors.CLASS_CANNOT_BE_EXTENDED_DIRECTLY, symbol)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkProjectionInImmediateArgumentToSupertype(
        coneType: ConeKotlinType,
        superTypeRef: FirTypeRef,
    ): Boolean {
        val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(superTypeRef) ?: return false
        var result = false
        for ((index, typeArgument) in coneType.typeArguments.withIndex()) {
            if (typeArgument.isConflictingOrNotInvariant) {
                val (_, argSource) = typeRefAndSourcesForArguments.getOrNull(index) ?: continue
                reporter.reportOn(
                    argSource ?: superTypeRef.source,
                    FirErrors.PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE
                )
                result = true
            }
        }
        return result
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkSupertypeOnTypeAliasWithTypeProjection(
        coneType: ConeKotlinType,
        fullyExpandedType: ConeKotlinType,
        superTypeRef: FirTypeRef,
    ) {
        if (coneType.toSymbol() is FirTypeAliasSymbol &&
            fullyExpandedType.typeArguments.any { it.isConflictingOrNotInvariant }
        ) {
            reporter.reportOn(superTypeRef.source, FirErrors.CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDelegationNotToInterface(
        declaration: FirClass,
    ) {
        @OptIn(DirectDeclarationsAccess::class)
        for (subDeclaration in declaration.declarations) {
            if (subDeclaration is FirField) {
                if (subDeclaration.visibility == Visibilities.Private && subDeclaration.name.isDelegated) {
                    val delegatedClassSymbol = subDeclaration.returnTypeRef.toRegularClassSymbol(context.session)
                    if (delegatedClassSymbol != null && delegatedClassSymbol.classKind != ClassKind.INTERFACE) {
                        reporter.reportOn(subDeclaration.returnTypeRef.source, FirErrors.DELEGATION_NOT_TO_INTERFACE)
                    }
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNamedFunctionTypeParameter(
        superTypeRef: FirTypeRef,
    ) {
        val delegatedTypeRef = (superTypeRef as? FirResolvedTypeRef)?.delegatedTypeRef ?: return
        if (delegatedTypeRef !is FirFunctionTypeRef) return
        for (parameter in delegatedTypeRef.parameters) {
            if (parameter.name != null) {
                val source = parameter.findSourceForParameterName() ?: continue
                reporter.reportOn(
                    source,
                    FirErrors.UNSUPPORTED,
                    "Named parameter in function type as supertype is unsupported."
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


    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDelegationWithoutPrimaryConstructor(
        declaration: FirClass,
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
                    "Delegation without primary constructor is unsupported."
                )
            }
        }
    }

}
