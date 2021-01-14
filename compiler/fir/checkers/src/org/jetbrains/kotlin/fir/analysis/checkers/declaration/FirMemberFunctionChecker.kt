/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name

// See old FE's [DeclarationsChecker]
object FirMemberFunctionChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (member in declaration.declarations) {
            if (member is FirSimpleFunction) {
                checkFunction(declaration, member, context, reporter)
            }
        }
    }

    private fun checkFunction(
        containingDeclaration: FirRegularClass,
        function: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val source = function.source ?: return
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = with(FirModifierList) { source.getModifierList() }
        val isAbstract = function.isAbstract || modifierList?.modifiers?.any { it.token == KtTokens.ABSTRACT_KEYWORD } == true
        if (isAbstract) {
            if (!containingDeclaration.canHaveAbstractDeclaration) {
                reporter.report(source, FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS)
            }
            if (function.hasBody) {
                reporter.report(source, FirErrors.ABSTRACT_FUNCTION_WITH_BODY)
            }
        }
        val isInsideExpectClass = isInsideExpectClass(containingDeclaration, context)
        val isOpen = function.isOpen || modifierList?.modifiers?.any { it.token == KtTokens.OPEN_KEYWORD } == true
        val isExternal = function.isExternal || modifierList?.modifiers?.any { it.token == KtTokens.EXTERNAL_KEYWORD } == true
        if (!function.hasBody && !function.isSyntheticMemberOfDataClass(containingDeclaration, context)) {
            if (containingDeclaration.isInterface) {
                if (Visibilities.isPrivate(function.visibility)) {
                    reporter.report(source, FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY)
                }
                if (!isInsideExpectClass && !isAbstract && isOpen) {
                    reporter.report(source, FirErrors.REDUNDANT_OPEN_IN_INTERFACE)
                }
            } else if (!isInsideExpectClass && !isAbstract && !isExternal) {
                // TODO: we need to check if modifiers of the function already get some errors, e.g., INCOMPATIBLE_MODIFIERS
                reporter.report(FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(source, function.symbol))
            }
        }
    }

    private fun isInsideExpectClass(containingDeclaration: FirRegularClass, context: CheckerContext): Boolean =
        // Note that the class that contains the currently visiting function is *not* in the context's containing declarations *yet*.
        containingDeclaration.isExpect || context.containingDeclarations.asReversed().any { it is FirRegularClass && it.isExpect }

    // We should not report errors for synthetic members of data class, since their body will be generated during FIR-2-IR.
    private fun FirSimpleFunction.isSyntheticMemberOfDataClass(containingDeclaration: FirRegularClass, context: CheckerContext): Boolean =
        containingDeclaration.isData &&
                ((name == equalsName && matchesEqualsSignature(context)) ||
                        (name == hashCodeName && matchesHashCodeSignature(context)) ||
                        (name == toStringName && matchesToStringSignature(context)) ||
                        (name == copyName && matchesCopySignature(containingDeclaration)) ||
                        (!name.isSpecial && name.identifier.startsWith("component") &&
                                componentIndex?.let { matchesComponentNSignature(containingDeclaration, it) } == true))

    private fun FirSimpleFunction.matchesEqualsSignature(context: CheckerContext): Boolean =
        valueParameters.size == 1 &&
                valueParameters[0].returnTypeRef == context.session.builtinTypes.nullableAnyType &&
                returnTypeRef == context.session.builtinTypes.booleanType

    private fun FirSimpleFunction.matchesHashCodeSignature(context: CheckerContext): Boolean =
        valueParameters.isEmpty() &&
                returnTypeRef == context.session.builtinTypes.intType

    private fun FirSimpleFunction.matchesToStringSignature(context: CheckerContext): Boolean =
        valueParameters.isEmpty() &&
                returnTypeRef == context.session.builtinTypes.stringType

    private fun FirSimpleFunction.matchesCopySignature(containingDeclaration: FirRegularClass): Boolean {
        val primaryConstructor = containingDeclaration.getPrimaryConstructorIfAny() ?: return false
        return returnTypeRef.coneType weakEq primaryConstructor.returnTypeRef.coneType &&
                (valueParameters.size == primaryConstructor.valueParameters.size &&
                        valueParameters.zip(primaryConstructor.valueParameters).all { pair ->
                            // vararg parameter is not allowed, but will be reported as DATA_CLASS_VARARG_PARAMETER
                            pair.first.returnTypeRef.coneType weakEq pair.second.returnTypeRef.coneType || pair.second.isVararg
                        }) ||
                // non-property parameter is not allowed, but will be reported as DATA_CLASS_NOT_PROPERTY_PARAMETER
                valueParameters.size < primaryConstructor.valueParameters.size
    }

    private fun FirSimpleFunction.matchesComponentNSignature(containingDeclaration: FirRegularClass, index: Int): Boolean {
        val primaryConstructor = containingDeclaration.getPrimaryConstructorIfAny() ?: return false
        val parameter = primaryConstructor.valueParameters.getOrNull(index - 1) ?: return false
        return valueParameters.isEmpty() &&
                // vararg is not allowed, but will be reported as DATA_CLASS_VARARG_PARAMETER
                (returnTypeRef.coneType weakEq parameter.returnTypeRef.coneType || parameter.isVararg)
    }

    // We can't use reference eq directly, since the one from the synthetic function has fake source, while the other has real source.
    // Also, we should allow error types from non-synthetic parts. Otherwise, we will report a false alarm and/or double alarms.
    private infix fun ConeKotlinType.weakEq(other: ConeKotlinType): Boolean =
        this == other || other is ConeClassErrorType

    private val FirSimpleFunction.componentIndex: Int?
        get() = if (name.isSpecial) null else name.identifier.substring("component".length).toIntOrNull()

    private val equalsName = Name.identifier("equals")
    private val hashCodeName = Name.identifier("hashCode")
    private val toStringName = Name.identifier("toString")
    private val copyName = Name.identifier("copy")
}
