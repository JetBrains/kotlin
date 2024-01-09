/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.superClassNotAny
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalChecker
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNative
import org.jetbrains.kotlin.psi.KtParameter

object FirJsExternalChecker : FirWebCommonExternalChecker() {
    override fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isNativeObject(session)
    }

    override fun reportExternalEnum(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(declaration.source, FirJsErrors.ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING, context)
    }

    override fun additionalCheck(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
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
            reporter.reportOn(declaration.source, FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION, context)
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
                val ktParam = parameter.source
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

        declaration.checkEnumEntry(context, reporter)
    }

    override fun isDefinedExternallyCallableId(callableId: CallableId): Boolean {
        return callableId in JsStandardClassIds.Callables.definedExternallyPropertyNames
    }

    override fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean {
        return declaration.hasAnnotation(JsNative, session)
    }

    private val KtSourceElement.allowsReporting
        get() = kind !is KtFakeSourceElementKind || kind == KtFakeSourceElementKind.PropertyFromParameter

    private val FirValueParameter.varargElementType
        get() = when {
            !isVararg -> null
            else -> returnTypeRef.coneType.typeArguments.firstOrNull()?.type
        }

    private fun FirClass.superInterfaces(session: FirSession) = superConeTypes
        .filterNot { it.isAny || it.isNullableAny }
        .filter { it.toSymbol(session)?.classKind == ClassKind.INTERFACE }

    private fun FirDeclaration.checkEnumEntry(context: CheckerContext, reporter: DiagnosticReporter) {
        if (this !is FirEnumEntry) return
        initializer?.let {
            reporter.reportOn(it.source, FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY, context)
        }
    }
}


