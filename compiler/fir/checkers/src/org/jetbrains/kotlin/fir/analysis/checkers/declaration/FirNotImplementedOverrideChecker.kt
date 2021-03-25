/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.HASHCODE_NAME
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
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
        val invisibleSymbols = mutableListOf<FirCallableSymbol<*>>()
        val classPackage = declaration.symbol.classId.packageFqName

        fun FirCallableMemberDeclaration<*>.isInvisible(): Boolean {
            if (visibility == Visibilities.Private ||
                !visibility.visibleFromPackage(classPackage, symbol.callableId.packageName)
            ) return true
            if (visibility == Visibilities.Internal &&
                session !== declaration.session
            ) return true
            return false
        }

        fun FirCallableMemberDeclaration<*>.shouldBeImplemented(): Boolean {
            if (!isAbstract) return false
            val containingClass = getContainingClass(context)
            if (containingClass === declaration) return false
            if (containingClass is FirRegularClass && containingClass.isExpect) return false
            return true
        }

        for (name in classScope.getCallableNames()) {
            classScope.processFunctionsByName(name) { namedFunctionSymbol ->
                val simpleFunction = namedFunctionSymbol.fir
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
                if (!property.shouldBeImplemented()) return@processPropertiesByName

                if (property.isInvisible()) {
                    invisibleSymbols += propertySymbol
                } else {
                    notImplementedSymbols += propertySymbol
                }
            }
        }

        if (notImplementedSymbols.isNotEmpty()) {
            val notImplemented = notImplementedSymbols.first().fir
            if (notImplemented.isFromInterfaceOrEnum(context)) {
                reporter.reportOn(source, FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            } else {
                reporter.reportOn(source, FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            }
        }
        if (invisibleSymbols.isNotEmpty()) {
            val invisible = invisibleSymbols.first().fir
            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses)) {
                reporter.reportOn(source, FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER, declaration, invisible, context)
            } else {
                reporter.reportOn(source, FirErrors.INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING, declaration, invisible, context)
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
