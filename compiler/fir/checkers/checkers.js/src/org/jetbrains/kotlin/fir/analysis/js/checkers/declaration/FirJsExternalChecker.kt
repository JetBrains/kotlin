/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.analysis.*
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsNative
import org.jetbrains.kotlin.name.StandardClassIds.Callables.JsDefinedExternally
import org.jetbrains.kotlin.name.StandardClassIds.Callables.JsNoImpl
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.keysToMap

@OptIn(SymbolInternals::class)
object FirJsExternalChecker : FirBasicDeclarationChecker() {
    val DEFINED_EXTERNALLY_PROPERTY_NAMES = setOf(JsNoImpl, JsDefinedExternally)

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val data = FirDeclarationWithParents(declaration, context.containingDeclarations, context)
        if (declaration is FirVariable && data.container is FirFunction) return
        if (!data.isNativeObject()) return

        if (!data.isTopLevelDeclaration) {
            if (declaration !is FirPropertyAccessor && declaration.isDirectlyExternal()) {
                reporter.reportOn(declaration.source, FirJsErrors.NESTED_EXTERNAL_DECLARATION, context)
            }
        }

        if (declaration is FirClass) {
            val classKind = when {
                declaration.status.isData -> "data class"
                declaration.status.isInner -> "inner class"
//                declaration.status.isInline -> "inline class"
//                declaration.status.isValue -> "value class"
                declaration.status.isInline -> "value class"
                declaration.status.isFun -> "fun interface"
                declaration.classKind == ClassKind.ANNOTATION_CLASS -> "annotation class"
                else -> null
            }

            if (classKind != null) {
                reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, classKind, context)
            }
        }

        if (declaration is FirPropertyAccessor && declaration.isDirectlyExternal()) {
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, "property accessor", context)
        } else if (declaration !is FirPrimaryConstructor && data.isPrivateMemberOfExternalClass()) {
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXTERNAL_DECLARATION, "private member of class", context)
        }

        if (
            declaration is FirClass &&
            declaration.classKind != ClassKind.INTERFACE &&
            data.container?.let { it is FirClass && it.classKind == ClassKind.INTERFACE } == true
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE, context)
        }

        if (declaration !is FirPropertyAccessor && declaration.isExtension) {
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
            val superDeclarations = superClasses
                .mapNotNull { it.toSymbol(context.session)?.fir }
                .map { FirDeclarationWithContext(it, context) }
            if (superDeclarations.any { !it.isNativeObject() && it.declaration.singleFqName != StandardNames.FqNames.throwable }) {
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

            val isAccessorWithSameTypeRefSource = declaration is FirPropertyAccessor
                    && declaration.returnTypeRef.source == declaration.propertySymbol?.fir?.returnTypeRef?.source

            if (!isAccessorWithSameTypeRefSource && declaration.source?.kind is KtRealSourceElementKind) {
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

                if (ktParam?.kind !is KtRealSourceElementKind) {
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

        reportOnParametersAndReturnTypesIf(valueClassInExternalDiagnostic) { it.isInlineClass(context.session) }

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JsEnableExtensionFunctionInExternals)) {
            reportOnParametersAndReturnTypesIf(
                FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, ConeKotlinType::isExtensionFunctionType
            )
        }

        if (
            declaration is FirCallableDeclaration &&
            data.isNonAbstractMemberIfInterface() &&
            !declaration.isNullableProperty()
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE, context)
        }

        data.checkBody(reporter)
        data.checkDelegation(reporter)
        data.checkAnonymousInitializer(reporter)
        data.checkEnumEntry(reporter)
        data.checkConstructorPropertyParam(reporter)
    }

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

    val FirDeclaration.isExtension
        get() = this is FirCallableDeclaration && receiverTypeRef != null

    val FirDeclarationWithContext<*>.isTopLevelDeclaration
        get() = container == null || container is FirFile

    private fun FirDeclarationWithContext<*>.checkBody(reporter: DiagnosticReporter) {
        if (declaration is FirDefaultPropertyAccessor) return

        val body = when (declaration) {
            is FirFunction -> declaration.body
            is FirAnonymousInitializer -> declaration.body
            else -> null
        }

        val initializer = when {
            declaration is FirEnumEntry -> null
            declaration.source?.kind == KtFakeSourceElementKind.PropertyFromParameter -> null
            declaration is FirVariable -> declaration.initializer
            body is FirSingleExpressionBlock -> (body.statement as? FirReturnExpression)?.result
            else -> null
        }

        val isWrong = body !is FirSingleExpressionBlock && !hasValidExternalBody()
                || initializer != null && !initializer.isDefinedExternallyExpression(context)

        if (isWrong && body != null) {
            reporter.reportOn(body.source, FirJsErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION, context)
        } else if (isWrong && initializer != null) {
            reporter.reportOn(initializer.source, FirJsErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION, context)
        }

        // we shouldn't check such things as the
        // copy() function of a data class
        if (declaration.source?.kind !is KtRealSourceElementKind) {
            return
        }

        if (declaration is FirFunction) {
            for (defaultValue in declaration.valueParameters.mapNotNull { it.defaultValue }) {
                if (!defaultValue.isDefinedExternallyExpression(context)) {
                    reporter.reportOn(defaultValue.source, FirJsErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER, context)
                }
            }
        }
    }

    private fun FirClass.collectSupertypesWithInfo(): Map<FirTypeRef, Boolean> {
        val isDelegatedSupertype = superConeTypes.keysToMap { false }.toMutableMap()

        for (it in declarations) {
            if (it is FirField && it.name.asString().startsWith("<$\$delegate_")) {
                val type = it.returnTypeRef.coneType as? ConeClassLikeType ?: continue
                isDelegatedSupertype[type] = true
            }
        }

        return superTypeRefs.zip(isDelegatedSupertype.values).toMap()
    }

    private fun FirDeclarationWithContext<*>.checkDelegation(reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration || !isEffectivelyExternal()) return

        if (declaration is FirClass) {
            for ((superType, isDelegated) in declaration.collectSupertypesWithInfo()) {
                val symbol = superType.coneType.toSymbol(session) as? FirClassSymbol<*>

                when {
                    symbol?.classKind == ClassKind.CLASS -> {
                        // TODO: report on valueArgumentsList
                        reporter.reportOn(superType.source, FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, context)
                    }
                    isDelegated -> {
                        reporter.reportOn(superType.source, FirJsErrors.EXTERNAL_DELEGATION, context)
                    }
                }
            }
        } else if (declaration is FirConstructor && !declaration.isPrimary) {
            val delegationCall = declaration.delegatedConstructor

            if (delegationCall?.source?.kind is KtRealSourceElementKind) {
                reporter.reportOn(delegationCall.source, FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, context)
            }
        } else if (declaration is FirProperty) {
            declaration.delegate?.let {
                reporter.reportOn(it.source, FirJsErrors.EXTERNAL_DELEGATION, context)
            }
        }
    }

    private fun FirDeclarationWithContext<*>.checkAnonymousInitializer(reporter: DiagnosticReporter) {
        if (declaration !is FirClass) return

        for (anonymousInitializer in declaration.anonymousInitializers) {
            reporter.reportOn(anonymousInitializer.source, FirJsErrors.EXTERNAL_ANONYMOUS_INITIALIZER, context)
        }
    }

    private fun FirDeclarationWithContext<*>.checkEnumEntry(reporter: DiagnosticReporter) {
        if (declaration !is FirEnumEntry) return
        declaration.initializer?.let {
            reporter.reportOn(it.source, FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY, context)
        }
    }

    private fun FirDeclarationWithContext<*>.checkConstructorPropertyParam(reporter: DiagnosticReporter) {
        if (declaration !is FirProperty || declaration.source?.kind != KtFakeSourceElementKind.PropertyFromParameter) return
        val containingClass = container as? FirClass ?: return
        if (containingClass.status.isData || containingClass.classKind == ClassKind.ANNOTATION_CLASS) return
        reporter.reportOn(declaration.source, FirJsErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, context)
    }

    private fun FirDeclaration.isDirectlyExternal(): Boolean {
        // source kind is checked, otherwise this function
        // may return true for a primary constructor of an external class
        if (this is FirDefaultPropertyAccessor || this.source?.kind !is KtRealSourceElementKind) return false
        return hasModifier(KtTokens.EXTERNAL_KEYWORD) || hasAnnotation(JsNative)
    }

    private fun FirDeclarationWithContext<*>.isPrivateMemberOfExternalClass(): Boolean {
        if (declaration is FirPropertyAccessor && declaration.visibility == declaration.propertySymbol?.visibility) return false
        if (declaration !is FirMemberDeclaration || declaration.visibility != Visibilities.Private) return false

        val containingDeclaration = parentClass ?: return false
        return containingDeclaration.isNativeObject()
    }

    private fun FirDeclarationWithContext<*>.isNonAbstractMemberIfInterface(): Boolean {
        return declaration is FirCallableDeclaration
                && declaration.modality != Modality.ABSTRACT
                && (container as? FirClass)?.classKind == ClassKind.INTERFACE
                && declaration !is FirPropertyAccessor
    }

    private fun FirCallableDeclaration.isNullableProperty() = this is FirProperty && returnTypeRef.coneType.isNullable

    private fun FirDeclarationWithContext<*>.hasValidExternalBody(): Boolean {
        val body = when (declaration) {
            is FirFunction -> declaration.body
            is FirAnonymousInitializer -> declaration.body
            else -> return true
        }

        return when {
            body is FirSingleExpressionBlock -> body.isDefinedExternallyExpression(context)
            body != null -> {
                val statement = body.statements.singleOrNull() ?: return false
                statement.isDefinedExternallyExpression(context)
            }
            else -> false
        }
    }

    private fun FirElement.isDefinedExternallyExpression(context: CheckerContext): Boolean {
        val declaration = (this as? FirPropertyAccessExpression)
            ?.calleeReference?.resolvedSymbol?.fir as? FirProperty ?: return false
        return DEFINED_EXTERNALLY_PROPERTY_NAMES.any {
            declaration.symbol.callableId.packageName == it.packageName && declaration.name == it.callableName
        }
    }
}