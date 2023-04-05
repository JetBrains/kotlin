/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNative
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

object FirJsExternalChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isNativeObject(context)) return

        if (!context.isTopLevel) {
            if (declaration !is FirPropertyAccessor && declaration.isDirectlyExternal(context.session)) {
                reporter.reportOn(declaration.source, FirJsErrors.NESTED_EXTERNAL_DECLARATION, context)
            }
        }

        if (declaration is FirClass) {
            // TODO: KT-55600: Stop generating diagnostic
            //  messages inside checkers
            val classKind = when {
                declaration.status.isData -> "data class"
                declaration.status.isInner -> "inner class"
                declaration.status.isInline -> "value class"
                declaration.status.isFun -> "fun interface"
                declaration.classKind == ClassKind.ANNOTATION_CLASS -> "annotation class"
                else -> null
            }

            if (classKind != null) {
                reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, classKind, context)
            }

            if (declaration.isEnumClass) {
                reporter.reportOn(declaration.source, FirJsErrors.ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, context)
            }
        }

        if (declaration is FirPropertyAccessor && declaration.isDirectlyExternal(context.session)) {
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, "property accessor", context)
        } else if (
            declaration !is FirPrimaryConstructor &&
            declaration !is FirField &&
            declaration.isPrivateMemberOfExternalClass(context.session)
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, "private member of class", context)
        }

        val container = context.containingDeclarations.lastOrNull()

        if (
            declaration is FirClass &&
            declaration.classKind != ClassKind.INTERFACE &&
            container is FirClass && container.classKind == ClassKind.INTERFACE
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE, context)
        }

        if (declaration !is FirPropertyAccessor && declaration is FirCallableDeclaration && declaration.isExtension) {
            val target = when (declaration) {
                is FirFunction -> "extension function"
                is FirProperty -> "extension property"
                else -> "extension member"
            }
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, target, context)
        }

        if (declaration is FirClass && declaration.classKind != ClassKind.ANNOTATION_CLASS) {
            val superClasses = declaration.superInterfaces(context.session).toMutableList()
            declaration.superClassNotAny(context.session)?.let {
                superClasses.add(it)
            }
            if (declaration.classKind == ClassKind.ENUM_CLASS || declaration.classKind == ClassKind.ENUM_ENTRY) {
                superClasses.removeAll { it.classId?.asSingleFqName()?.toUnsafe() == StandardNames.FqNames._enum }
            }
            val superDeclarations = superClasses.mapNotNull { it.toSymbol(context.session) }
            if (superDeclarations.any { !it.isNativeObject(context) && it.classId.asSingleFqName() != StandardNames.FqNames.throwable }) {
                reporter.reportOn(declaration.source, FirJsErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE, context)
            }
        }

        if (declaration is FirFunction && declaration.isInline) {
            reporter.reportOn(declaration.source, FirJsErrors.INLINE_EXTERNAL_DECLARATION, context)
        }

        fun reportOnParametersAndReturnTypesIf(
            diagnosticFactory: KtDiagnosticFactory0,
            condition: (ConeKotlinType) -> Boolean,
        ) {
            if (
                declaration !is FirCallableDeclaration ||
                declaration is FirDefaultPropertyAccessor ||
                declaration is FirDefaultPropertyBackingField
            ) {
                return
            }

            fun checkTypeIsNotInlineClass(type: ConeKotlinType, elementToReport: KtSourceElement?) {
                if (condition(type)) {
                    reporter.reportOn(elementToReport, diagnosticFactory, context)
                }
            }

            if (declaration.returnTypeRef.source?.allowsReporting == true) {
                declaration.returnTypeRef.source?.let {
                    checkTypeIsNotInlineClass(declaration.returnTypeRef.coneType, it)
                }
            }

            if (declaration !is FirFunction) {
                return
            }

            for (parameter in declaration.valueParameters) {
                val ktParam = if (parameter.source?.psi is KtParameter) {
                    parameter.source
                } else {
                    declaration.source
                }

                if (ktParam?.allowsReporting != true) {
                    continue
                }

                val typeToCheck = parameter.varargElementType ?: parameter.returnTypeRef.coneType
                checkTypeIsNotInlineClass(typeToCheck, ktParam)
            }
        }

        val valueClassInExternalDiagnostic = when {
            context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowValueClassesInExternals) -> {
                FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING
            }

            else -> {
                FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION
            }
        }

        reportOnParametersAndReturnTypesIf(valueClassInExternalDiagnostic) { it.isValueClass(context.session) }

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JsEnableExtensionFunctionInExternals)) {
            reportOnParametersAndReturnTypesIf(
                FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, ConeKotlinType::isExtensionFunctionType
            )
        }

        if (
            declaration is FirCallableDeclaration &&
            declaration.isNonAbstractMemberIfInterface(context.session) &&
            !declaration.isNullableProperty()
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE, context)
        }

        declaration.checkBody(context, reporter)
        declaration.checkDelegation(context, reporter)
        declaration.checkAnonymousInitializer(context, reporter)
        declaration.checkEnumEntry(context, reporter)
        declaration.checkConstructorPropertyParam(context, reporter)
   }

    private val KtSourceElement.allowsReporting
        get() = kind !is KtFakeSourceElementKind || kind == KtFakeSourceElementKind.PropertyFromParameter

    private val FirValueParameter.varargElementType
        get() = when {
            !isVararg -> null
            else -> returnTypeRef.coneType.typeArguments.firstOrNull()?.type
        }

    private fun FirClass.superClassNotAny(session: FirSession) = superConeTypes
        .filterNot { it.isAny || it.isNullableAny }
        .find { it.toSymbol(session)?.classKind == ClassKind.CLASS }

    private fun FirClass.superInterfaces(session: FirSession) = superConeTypes
        .filterNot { it.isAny || it.isNullableAny }
        .filter { it.toSymbol(session)?.classKind == ClassKind.INTERFACE }

    private fun FirDeclaration.checkBody(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this is FirDefaultPropertyAccessor) return

        val body = when (this) {
            is FirFunction -> body
            is FirAnonymousInitializer -> body
            else -> null
        }

        val initializer = when {
            this is FirEnumEntry -> null
            source?.kind == KtFakeSourceElementKind.PropertyFromParameter -> null
            this is FirVariable -> initializer
            body is FirSingleExpressionBlock -> (body.statement as? FirReturnExpression)?.result
            else -> null
        }

        // we shouldn't check such things as the
        // copy() function of a data class
        if (source?.kind !is KtRealSourceElementKind) {
            return
        }

        val isWrong = body !is FirSingleExpressionBlock && !hasValidExternalBody()
                || initializer != null && !initializer.isDefinedExternallyExpression()

        if (isWrong && body != null) {
            reporter.reportOn(body.source, FirJsErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION, context)
        } else if (isWrong && initializer != null) {
            reporter.reportOn(initializer.source, FirJsErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION, context)
        }

        if (this is FirFunction) {
            for (defaultValue in valueParameters.mapNotNull { it.defaultValue }) {
                if (!defaultValue.isDefinedExternallyExpression()) {
                    reporter.reportOn(defaultValue.source, FirJsErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER, context)
                }
            }
        }
    }

    private fun FirDeclaration.checkDelegation(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this !is FirMemberDeclaration || !symbol.isEffectivelyExternal(context)) return

        if (this is FirClass) {
            declarations.firstIsInstanceOrNull<FirPrimaryConstructor>()?.let {
                val constructorCall = it.delegatedConstructor

                if (constructorCall?.source?.kind is KtRealSourceElementKind) {
                    reporter.reportOn(constructorCall.source, FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, context)
                }
            }

            for ((superType, delegate) in collectSupertypesWithDelegates()) {
                when {
                    delegate != null -> {
                        reporter.reportOn(superType.source, FirJsErrors.EXTERNAL_DELEGATION, context)
                    }
                }
            }
        } else if (this is FirConstructor && !isPrimary) {
            val delegationCall = delegatedConstructor

            if (delegationCall?.source?.kind is KtRealSourceElementKind) {
                reporter.reportOn(delegationCall.source, FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, context)
            }
        } else if (this is FirProperty) {
            delegate?.let {
                reporter.reportOn(it.source, FirJsErrors.EXTERNAL_DELEGATION, context)
            }
        }
    }

    private fun FirDeclaration.checkAnonymousInitializer(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this !is FirClass) return

        for (anonymousInitializer in anonymousInitializers) {
            reporter.reportOn(anonymousInitializer.source, FirJsErrors.EXTERNAL_ANONYMOUS_INITIALIZER, context)
        }
    }

    private fun FirDeclaration.checkEnumEntry(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this !is FirEnumEntry) return
        initializer?.let {
            reporter.reportOn(it.source, FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY, context)
        }
    }

    private fun FirDeclaration.checkConstructorPropertyParam(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this !is FirProperty || source?.kind != KtFakeSourceElementKind.PropertyFromParameter) return
        val containingClass = getContainingClassSymbol(context.session) as? FirClassSymbol<*> ?: return
        if (containingClass.isData || containingClass.classKind == ClassKind.ANNOTATION_CLASS) return
        reporter.reportOn(source, FirJsErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, context)
    }

    private fun FirDeclaration.isDirectlyExternal(session: FirSession): Boolean {
        // source kind is checked, otherwise this function
        // may return true for a primary constructor of an external class
        if (this is FirDefaultPropertyAccessor || this.source?.kind !is KtRealSourceElementKind) return false
        return hasModifier(KtTokens.EXTERNAL_KEYWORD) || hasAnnotation(JsNative, session)
    }

    private fun FirDeclaration.isPrivateMemberOfExternalClass(session: FirSession): Boolean {
        if (this is FirPropertyAccessor && visibility == propertySymbol.visibility) return false
        if (this !is FirMemberDeclaration || visibility != Visibilities.Private) return false

        val containingDeclaration = getContainingClassSymbol(session) ?: return false
        return containingDeclaration.isNativeObject(session)
    }

    private fun FirDeclaration.isNonAbstractMemberIfInterface(session: FirSession): Boolean {
        return this is FirCallableDeclaration
                && modality != Modality.ABSTRACT
                && (getContainingClassSymbol(session) as? FirClassSymbol<*>)?.classKind == ClassKind.INTERFACE
                && this !is FirPropertyAccessor
    }

    private fun FirCallableDeclaration.isNullableProperty() = this is FirProperty && returnTypeRef.coneType.isNullable

    private fun FirDeclaration.hasValidExternalBody(): Boolean {
        val body = when (this) {
            is FirFunction -> body
            is FirAnonymousInitializer -> body
            else -> return true
        }

        return when {
            body is FirSingleExpressionBlock -> body.isDefinedExternallyExpression()
            body != null -> {
                val statement = body.statements.singleOrNull() ?: return false
                statement.isDefinedExternallyExpression()
            }

            else -> false
        }
    }

    private fun FirElement.isDefinedExternallyExpression(): Boolean {
        val declaration = (this as? FirPropertyAccessExpression)
            ?.calleeReference?.toResolvedPropertySymbol() ?: return false
        return declaration.callableId in JsStandardClassIds.Callables.definedExternallyPropertyNames
    }
}