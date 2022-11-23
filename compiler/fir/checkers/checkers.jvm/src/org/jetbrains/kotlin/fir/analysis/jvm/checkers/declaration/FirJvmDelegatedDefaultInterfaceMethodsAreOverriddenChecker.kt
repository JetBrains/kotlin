/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctionsWithBaseScope
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.scopes.impl.filterOutOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirJvmDelegatedDefaultInterfaceMethodsAreOverriddenChecker: FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.allowImplicitDelegationToDefaults)) return
        val source = declaration.source ?: return
        val sourceKind = source.kind
        if (sourceKind is KtFakeSourceElementKind && sourceKind != KtFakeSourceElementKind.EnumInitializer) return
        if (declaration is FirRegularClass && declaration.isExpect) return
        if (declaration.classKind == ClassKind.ANNOTATION_CLASS) return
        val classScope = declaration.unsubstitutedScope(context)
        var reported = false

        classScope.processAllFunctions { symbol ->
            if (reported) return@processAllFunctions
            val delegatedWrapperData = symbol.delegatedWrapperData ?: return@processAllFunctions
            if (delegatedWrapperData.containingClass.classId != declaration.classId) return@processAllFunctions
            if (delegatedWrapperData.delegateField.initializer?.annotations?.any {
                    it.typeRef.toRegularClassSymbol(context.session)?.classId?.asFqNameString() == "kotlin.jvm.JvmDelegateToDefaults"
                } == true) return@processAllFunctions
            val nonOverriddenDefault = filterOutOverriddenFunctions(classScope.getDirectOverriddenFunctionsWithBaseScope(symbol))
                .map { it.member }
                .firstOrNull { isDefaultJavaMethod(it, context.session) }
            nonOverriddenDefault?.let {
                reporter.reportOn(source, FirErrors.NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD, it, context)
                reported = true
            }
        }
    }

    private tailrec fun isDefaultJavaMethod(callable: FirCallableSymbol<*>, session: FirSession): Boolean =
        when {
            callable.isIntersectionOverride -> isDefaultJavaMethod(callable.baseForIntersectionOverride!!, session)
            callable.isSubstitutionOverride -> isDefaultJavaMethod(callable.originalForSubstitutionOverride!!, session)
            else -> callable.dispatchReceiverType?.toRegularClassSymbol(session)?.classKind == ClassKind.INTERFACE
                    && callable.origin == FirDeclarationOrigin.Enhancement && callable.modality == Modality.OPEN
        }
}

