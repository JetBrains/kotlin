/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OVERRIDING_FINAL_MEMBER_BY_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverridePropertySymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirNotImplementedOverrideChecker : FirClassChecker() {

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is FirFakeSourceElementKind && sourceKind != FirFakeSourceElementKind.EnumInitializer) return
        val modality = declaration.modality()
        if (modality == Modality.ABSTRACT || modality == Modality.SEALED) return
        if (declaration is FirRegularClass && declaration.isExpect) return
        val classKind = declaration.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val classScope = declaration.unsubstitutedScope(context)

        val notImplementedSymbols = mutableListOf<FirCallableSymbol<*>>()
        val notImplementedIntersectionSymbols = mutableListOf<FirCallableSymbol<*>>()
        val invisibleSymbols = mutableListOf<FirCallableSymbol<*>>()
        val classPackage = declaration.symbol.classId.packageFqName

        fun FirCallableMemberDeclaration<*>.isInvisible(): Boolean {
            if (visibility == Visibilities.Private ||
                !visibility.visibleFromPackage(classPackage, symbol.callableId.packageName)
            ) return true
            if (visibility == Visibilities.Internal &&
                declarationSiteSession !== declaration.declarationSiteSession
            ) return true
            return false
        }

        fun FirIntersectionCallableSymbol.subjectToManyNotImplemented(): Boolean {
            var nonAbstractCountInClass = 0
            var nonAbstractCountInInterface = 0
            var abstractCountInInterface = 0
            for (intersectionSymbol in intersections) {
                val intersection = intersectionSymbol.fir as FirCallableMemberDeclaration
                val containingClass = intersection.getContainingClass(context) as? FirRegularClass
                val hasInterfaceContainer = containingClass?.classKind == ClassKind.INTERFACE
                if (intersection.modality != Modality.ABSTRACT) {
                    if (hasInterfaceContainer) {
                        nonAbstractCountInInterface++
                    } else {
                        nonAbstractCountInClass++
                    }
                } else if (hasInterfaceContainer) {
                    abstractCountInInterface++
                }
                if (nonAbstractCountInClass + nonAbstractCountInInterface > 1) {
                    return true
                }
                if (nonAbstractCountInInterface > 0 && abstractCountInInterface > 0) {
                    return true
                }
            }
            return false
        }

        fun FirIntersectionCallableSymbol.shouldBeImplemented(): Boolean {
            // In Java 8, non-abstract intersection overrides having abstract symbol from base class
            // still should be implemented in current class (even when they have default interface implementation)
            if (intersections.none {
                    val fir = (it.fir as FirCallableMemberDeclaration).unwrapFakeOverrides()
                    fir.isAbstract && (fir.getContainingClass(context) as? FirRegularClass)?.classKind == ClassKind.CLASS
                }
            ) return false
            // Exception from the rule above: interface implementation via delegation
            if (intersections.any {
                    val fir = (it.fir as FirCallableMemberDeclaration)
                    fir.origin == FirDeclarationOrigin.Delegated && !fir.isAbstract
                }
            ) return false
            return true
        }

        fun FirCallableMemberDeclaration<*>.shouldBeImplemented(): Boolean {
            val containingClass = getContainingClass(context)
            if (containingClass is FirRegularClass && containingClass.isExpect) return false
            if (!isAbstract) {
                val symbol = symbol as? FirIntersectionCallableSymbol ?: return false
                return symbol.shouldBeImplemented()
            }
            if (containingClass === declaration && origin == FirDeclarationOrigin.Source) return false
            return true
        }

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name) { namedFunctionSymbol ->
                val simpleFunction = namedFunctionSymbol.fir
                if (namedFunctionSymbol is FirIntersectionOverrideFunctionSymbol) {
                    if (simpleFunction.getContainingClass(context) === declaration &&
                        namedFunctionSymbol.subjectToManyNotImplemented()
                    ) {
                        notImplementedIntersectionSymbols += namedFunctionSymbol
                        return@processFunctionsByName
                    }
                }
                if (!simpleFunction.shouldBeImplemented()) return@processFunctionsByName
                if (declaration is FirRegularClass && declaration.isData && simpleFunction.matchesDataClassSyntheticMemberSignatures) {
                    return@processFunctionsByName
                }

                // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override
                if (simpleFunction.isSuspend) return@processFunctionsByName
                if (simpleFunction.isInvisible()) {
                    invisibleSymbols += namedFunctionSymbol
                } else {
                    notImplementedSymbols += namedFunctionSymbol
                }
            }
            classScope.processPropertiesByName(name) { propertySymbol ->
                val property = propertySymbol.fir as? FirProperty ?: return@processPropertiesByName
                if (propertySymbol is FirIntersectionOverridePropertySymbol) {
                    if (property.getContainingClass(context) === declaration &&
                        propertySymbol.subjectToManyNotImplemented()
                    ) {
                        notImplementedIntersectionSymbols += propertySymbol
                        return@processPropertiesByName
                    }
                }
                if (!property.shouldBeImplemented()) return@processPropertiesByName

                if (property.isInvisible()) {
                    invisibleSymbols += propertySymbol
                } else {
                    notImplementedSymbols += propertySymbol
                }
            }
        }

        if (notImplementedSymbols.isNotEmpty()) {
            val notImplemented = notImplementedSymbols.first().unwrapFakeOverrides().fir
            if (notImplemented.isFromInterfaceOrEnum(context)) {
                reporter.reportOn(source, ABSTRACT_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            } else {
                reporter.reportOn(source, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            }
        }
        if (invisibleSymbols.isNotEmpty()) {
            val invisible = invisibleSymbols.first().fir
            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses)) {
                reporter.reportOn(source, INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER, declaration, invisible, context)
            } else {
                reporter.reportOn(source, INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING, declaration, invisible, context)
            }
        }
        if (notImplementedIntersectionSymbols.isNotEmpty()) {
            var overridingFinalByDelegationReported = false
            var manyMemberNotImplementedReported = false
            var delegatedHidesSupertypeReported = false
            for (notImplementedIntersectionSymbol in notImplementedIntersectionSymbols) {
                val notImplementedIntersection = notImplementedIntersectionSymbol.fir
                val intersections = (notImplementedIntersectionSymbol as FirIntersectionCallableSymbol).intersections
                val delegatedIntersected = intersections.find {
                    val fir = it.fir as FirCallableMemberDeclaration
                    fir.origin == FirDeclarationOrigin.Delegated
                }
                if (delegatedIntersected != null) {
                    val finalIntersected = intersections.find { (it.fir as FirCallableMemberDeclaration).modality == Modality.FINAL }
                    if (finalIntersected != null) {
                        if (!overridingFinalByDelegationReported) {
                            reporter.reportOn(
                                source,
                                OVERRIDING_FINAL_MEMBER_BY_DELEGATION,
                                delegatedIntersected.fir,
                                finalIntersected.fir,
                                context
                            )
                            overridingFinalByDelegationReported = true
                        }
                        continue
                    }
                    val notDelegatedIntersected = intersections.firstOrNull {
                        (it.fir as FirCallableMemberDeclaration).origin != FirDeclarationOrigin.Delegated
                    }
                    if (notDelegatedIntersected != null) {
                        if (!delegatedHidesSupertypeReported) {
                            reporter.reportOn(
                                source,
                                DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE,
                                delegatedIntersected.fir,
                                notDelegatedIntersected.fir,
                                context
                            )
                            delegatedHidesSupertypeReported = true
                        }
                        continue
                    }
                }
                if (manyMemberNotImplementedReported) continue
                if (intersections.any {
                        (it.containingClass()?.toSymbol(context.session)?.fir as? FirRegularClass)?.classKind == ClassKind.CLASS
                    }
                ) {
                    reporter.reportOn(source, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, declaration, notImplementedIntersection, context)
                } else {
                    reporter.reportOn(source, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED, declaration, notImplementedIntersection, context)
                }
                manyMemberNotImplementedReported = true
            }
        }
    }

    private fun FirCallableDeclaration<*>.isFromInterfaceOrEnum(context: CheckerContext): Boolean =
        (getContainingClass(context) as? FirRegularClass)?.let { it.isInterface || it.isEnumClass } == true

    private val FirSimpleFunction.matchesDataClassSyntheticMemberSignatures: Boolean
        get() = (this.name == OperatorNameConventions.EQUALS && matchesEqualsSignature) ||
                (this.name == HASHCODE_NAME && matchesHashCodeSignature) ||
                (this.name == OperatorNameConventions.TO_STRING && matchesToStringSignature)

    // NB: we intentionally do not check return types
    private val FirSimpleFunction.matchesEqualsSignature: Boolean
        get() = valueParameters.size == 1 && valueParameters[0].returnTypeRef.coneType.isNullableAny

    private val FirSimpleFunction.matchesHashCodeSignature: Boolean
        get() = valueParameters.isEmpty()

    private val FirSimpleFunction.matchesToStringSignature: Boolean
        get() = valueParameters.isEmpty()
}
