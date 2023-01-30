/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.Returns
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.ValueParametersCount
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.isKProperty
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.member
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.memberOrExtension
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.Checks.noDefaultAndVarargs
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.ASSIGNMENT_OPERATIONS
import org.jetbrains.kotlin.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPARE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPONENT_REGEX
import org.jetbrains.kotlin.util.OperatorNameConventions.CONTAINS
import org.jetbrains.kotlin.util.OperatorNameConventions.DEC
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.GET
import org.jetbrains.kotlin.util.OperatorNameConventions.GET_VALUE
import org.jetbrains.kotlin.util.OperatorNameConventions.HAS_NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.INC
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE
import org.jetbrains.kotlin.util.OperatorNameConventions.ITERATOR
import org.jetbrains.kotlin.util.OperatorNameConventions.NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.PROVIDE_DELEGATE
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_UNTIL
import org.jetbrains.kotlin.util.OperatorNameConventions.SET
import org.jetbrains.kotlin.util.OperatorNameConventions.SET_VALUE
import org.jetbrains.kotlin.util.OperatorNameConventions.SIMPLE_UNARY_OPERATION_NAMES


object FirOperatorModifierChecker : FirSimpleFunctionChecker() {

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isOperator) return
        //we are not interested in implicit operators from override
        if (!declaration.hasModifier(KtTokens.OPERATOR_KEYWORD)) return

        val checks = OperatorFunctionChecks.checksByName.getOrElse(declaration.name) {
            OperatorFunctionChecks.regexChecks.find { it.first.matches(declaration.name.asString()) }?.second
        }

        if (checks == null) {
            reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, "illegal function name", context)
            return
        }

        for (check in checks) {
            check.check(context, declaration)?.let { error ->
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_OPERATOR_MODIFIER, error, context)
                return
            }
        }

        checkReplaceableLegacyOperators(declaration, context, reporter)
    }

    private fun checkReplaceableLegacyOperators(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val replacement = OperatorNameConventions.MOD_OPERATORS_REPLACEMENT[declaration.name] ?: return

        val diagnostic = if (
            declaration.symbol.callableId.packageName.isSubpackageOf(StandardClassIds.BASE_KOTLIN_PACKAGE) ||
            !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitOperatorMod)
        ) {
            FirErrors.DEPRECATED_BINARY_MOD
        } else {
            FirErrors.FORBIDDEN_BINARY_MOD
        }

        reporter.reportOn(declaration.source, diagnostic, declaration.symbol, replacement.asString(), context)
    }
}

private interface Check : (CheckerContext, FirSimpleFunction) -> String? {
    override fun invoke(p1: CheckerContext, p2: FirSimpleFunction): String? {
        return check(p1, p2)
    }

    fun check(context: CheckerContext, function: FirSimpleFunction): String?
}

private object Checks {
    fun simple(message: String, predicate: (FirSimpleFunction) -> Boolean) = object : Check {
        override fun check(context: CheckerContext, function: FirSimpleFunction): String? = message.takeIf { !predicate(function) }
    }

    fun full(message: String, predicate: (CheckerContext, FirSimpleFunction) -> Boolean) = object : Check {
        override fun check(context: CheckerContext, function: FirSimpleFunction): String? = message.takeIf { !predicate(context, function) }
    }

    val memberOrExtension = simple("must be a member or an extension function") {
        it.dispatchReceiverType != null || it.receiverParameter != null
    }

    val member = simple("must be a member function") {
        it.dispatchReceiverType != null
    }

    object ValueParametersCount {
        fun atLeast(n: Int) = simple("must have at least $n value parameter" + (if (n > 1) "s" else "")) {
            it.valueParameters.size >= n
        }

        fun exactly(n: Int) = simple("must have exactly $n value parameters") {
            it.valueParameters.size == n
        }

        val single = simple("must have a single value parameter") {
            it.valueParameters.size == 1
        }
        val none = simple("must have no value parameters") {
            it.valueParameters.isEmpty()
        }
    }

    object Returns {
        val boolean = simple("must return Boolean") {
            it.returnTypeRef.isBoolean
        }

        val int = simple("must return Int") {
            it.returnTypeRef.isInt
        }

        val unit = simple("must return Unit") {
            it.returnTypeRef.isUnit
        }
    }

    val noDefaultAndVarargs = simple("should not have varargs or parameters with default values") {
        it.valueParameters.all { param ->
            param.defaultValue == null && !param.isVararg
        }
    }

    val isKProperty = full("second parameter must be of type KProperty<*> or its supertype") { ctx, function ->
        val paramType = function.valueParameters[1].returnTypeRef.coneType
        paramType.isSupertypeOf(
            ctx.session.typeContext,
            ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(StandardClassIds.KProperty),
                arrayOf(ConeStarProjection),
                isNullable = false
            )
        )
    }

}

private object OperatorFunctionChecks {

    //reimplementation of org.jetbrains.kotlin.util.OperatorChecks for FIR
    val checksByName: Map<Name, List<Check>> = buildMap {
        checkFor(GET, memberOrExtension, ValueParametersCount.atLeast(1))
        checkFor(
            SET,
            memberOrExtension, ValueParametersCount.atLeast(2),
            Checks.simple("last parameter should not have a default value or be a vararg") {
                it.valueParameters.lastOrNull()?.let { param ->
                    param.defaultValue == null && !param.isVararg
                } == true
            }
        )
        checkFor(GET_VALUE, memberOrExtension, noDefaultAndVarargs, ValueParametersCount.atLeast(2), isKProperty)
        checkFor(SET_VALUE, memberOrExtension, noDefaultAndVarargs, ValueParametersCount.atLeast(3), isKProperty)
        checkFor(PROVIDE_DELEGATE, memberOrExtension, noDefaultAndVarargs, ValueParametersCount.exactly(2), isKProperty)
        checkFor(INVOKE, memberOrExtension)
        checkFor(CONTAINS, memberOrExtension, ValueParametersCount.single, noDefaultAndVarargs, Returns.boolean)
        checkFor(ITERATOR, memberOrExtension, ValueParametersCount.none)
        checkFor(NEXT, memberOrExtension, ValueParametersCount.none)
        checkFor(HAS_NEXT, memberOrExtension, ValueParametersCount.none, Returns.boolean)
        checkFor(RANGE_TO, memberOrExtension, ValueParametersCount.single, noDefaultAndVarargs)
        checkFor(RANGE_UNTIL, memberOrExtension, ValueParametersCount.single, noDefaultAndVarargs)
        checkFor(
            EQUALS,
            member,
            object : Check {
                override fun check(context: CheckerContext, function: FirSimpleFunction): String? {
                    val containingClassSymbol = function.containingClassLookupTag()?.toFirRegularClassSymbol(context.session) ?: return null
                    val customEqualsSupported = context.languageVersionSettings.supportsFeature(LanguageFeature.CustomEqualsInValueClasses)

                    if (function.overriddenFunctions(containingClassSymbol, context)
                            .any { it.containingClassLookupTag()?.classId == StandardClassIds.Any }
                        || (customEqualsSupported && function.isTypedEqualsInValueClass(context.session))
                    ) {
                        return null
                    }
                    return buildString {
                        append("must override ''equals()'' in Any")
                        if (customEqualsSupported && containingClassSymbol.isInline) {
                            val expectedParameterTypeRendered =
                                containingClassSymbol.defaultType().replaceArgumentsWithStarProjections().renderReadable();
                            append(" or define ''equals(other: ${expectedParameterTypeRendered}): Boolean''")
                        }
                    }
                }
            }
        )
        checkFor(COMPARE_TO, memberOrExtension, Returns.int, ValueParametersCount.single, noDefaultAndVarargs)
        checkFor(BINARY_OPERATION_NAMES, memberOrExtension, ValueParametersCount.single, noDefaultAndVarargs)
        checkFor(SIMPLE_UNARY_OPERATION_NAMES, memberOrExtension, ValueParametersCount.none)
        checkFor(
            setOf(INC, DEC),
            memberOrExtension,
            Checks.full("receiver must be a supertype of the return type") { ctx, function ->
                val receiver = function.dispatchReceiverType ?: function.receiverParameter?.typeRef?.coneType ?: return@full false
                function.returnTypeRef.coneType.isSubtypeOf(ctx.session.typeContext, receiver)
            }
        )
        checkFor(ASSIGNMENT_OPERATIONS, memberOrExtension, Returns.unit, ValueParametersCount.single, noDefaultAndVarargs)
    }

    val regexChecks: List<Pair<Regex, List<Check>>> = buildList {
        checkFor(COMPONENT_REGEX, memberOrExtension, ValueParametersCount.none)
    }

    private fun MutableMap<Name, List<Check>>.checkFor(name: Name, vararg checks: Check) {
        put(name, checks.asList())
    }

    private fun MutableMap<Name, List<Check>>.checkFor(names: Set<Name>, vararg checks: Check) {
        names.forEach { put(it, checks.asList()) }
    }

    private fun MutableList<Pair<Regex, List<Check>>>.checkFor(regex: Regex, vararg checks: Check) {
        add(regex to checks.asList())
    }
}
