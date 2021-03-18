/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.HASHCODE_NAME
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirNotImplementedOverrideChecker : FirClassChecker() {

    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        // TODO: kt4763Property: reporting on `object` literal causes invalid error in test...FirDiagnosticHandler
        if (declaration !is FirRegularClass) return

        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return
        val modality = declaration.modality()
        if (modality == Modality.ABSTRACT || modality == Modality.SEALED) return
        if (declaration.isExpect) return
        val classKind = declaration.classKind
        // TODO: we should check enum entries (probably as anonymous objects, see above)
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) return

        val classScope = declaration.unsubstitutedScope(
            context.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false
        )

        val notImplementedSymbols = mutableListOf<FirCallableSymbol<*>>()
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
                // TODO: private & package-private functions / properties require another diagnostic
                // (INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER)
                if (simpleFunction.isInvisible()) return@processFunctionsByName

                if (declaration.isData && simpleFunction.matchesDataClassSyntheticMemberSignatures) return@processFunctionsByName

                // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override
                if (simpleFunction.isSuspend) return@processFunctionsByName
                notImplementedSymbols += namedFunctionSymbol
            }
            classScope.processPropertiesByName(name) { propertySymbol ->
                val property = propertySymbol.fir as? FirProperty ?: return@processPropertiesByName
                if (!property.shouldBeImplemented()) return@processPropertiesByName
                if (property.isInvisible()) return@processPropertiesByName

                notImplementedSymbols += propertySymbol
            }
        }

        if (notImplementedSymbols.isNotEmpty()) {
            val notImplemented = notImplementedSymbols.first().fir
            if (notImplemented.isFromInterface(context)) {
                reporter.reportOn(source, FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            } else {
                reporter.reportOn(source, FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, declaration, notImplemented, context)
            }
        }
    }

    private fun FirCallableDeclaration<*>.isFromInterface(context: CheckerContext): Boolean =
        (getContainingClass(context) as? FirRegularClass)?.isInterface == true

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
