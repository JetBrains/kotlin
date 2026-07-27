/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.JvmFieldApplicabilityProblem.*
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.config.LanguageFeature.ForbidJvmAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.isInlineClassThatRequiresMangling
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_MULTIFILE_CLASS_ID

object FirJvmFieldApplicabilityChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val session = context.session
        val annotation = declaration.backingField?.getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID, session)
            ?: return
        val containingClassSymbol = declaration.containingClassLookupTag()?.toRegularClassSymbol()

        val problem = when {
            declaration.delegate != null -> DELEGATE
            !declaration.hasBackingField -> return
            declaration.isOverridable(containingClassSymbol) -> NOT_FINAL
            Visibilities.isPrivate(declaration.visibility) -> PRIVATE
            declaration.hasCustomAccessor() -> CUSTOM_ACCESSOR
            declaration.isOverride -> OVERRIDES
            declaration.isLateInit -> LATEINIT
            declaration.isConst -> CONST
            containingClassSymbol == null && isInsideJvmMultifileClassFile() ->
                TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE
            declaration.returnTypeRef.isInlineClassThatRequiresMangling(session) -> RETURN_TYPE_IS_VALUE_CLASS
            containingClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS -> ANNOTATION
            declaration.hasExplicitBackingField -> PROPERTY_WITH_EXPLICIT_FIELD
            else -> return
        }

        val factory = when {
            problem == ANNOTATION -> {
                when {
                    ForbidJvmAnnotationsOnAnnotationParameters.isDisabled() ->
                        FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING
                    ForbidFieldAnnotationsOnAnnotationParameters.isEnabled() ->
                        return
                    else ->
                        FirJvmErrors.INAPPLICABLE_JVM_FIELD
                }
            }
            else -> {
                FirJvmErrors.INAPPLICABLE_JVM_FIELD
            }
        }

        reporter.reportOn(annotation.source, factory, problem.errorMessage)
    }

    private fun FirProperty.isOverridable(containingClass: FirRegularClassSymbol?): Boolean {
        return visibility != Visibilities.Private && modality != Modality.FINAL &&
                containingClass?.isFinal != true
    }

    private fun FirProperty.hasCustomAccessor(): Boolean {
        return (getter != null && getter?.source?.kind !is KtFakeSourceElementKind) ||
                (setter != null && setter?.source?.kind !is KtFakeSourceElementKind)
    }

    context(context: CheckerContext)
    private fun isInsideJvmMultifileClassFile(): Boolean {
        return context.containingFileSymbol?.hasAnnotation(JVM_MULTIFILE_CLASS_ID, context.session) == true
    }
}

object FirInterfaceJvmFieldApplicabilityChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.isInterface && declaration.classKind != ClassKind.ANNOTATION_CLASS) return
        var hasIncompatible = false
        val jvmFieldAnnotations = mutableListOf<KtSourceElement>()

        val processor = processAllDeclaredCallables@{ symbol: FirCallableSymbol<*> ->
            if (symbol !is FirPropertySymbol) return@processAllDeclaredCallables
            val annotation = symbol.backingFieldSymbol?.getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID, context.session)

            if (annotation != null) jvmFieldAnnotations.add(annotation.source!!)

            if (symbol.visibility != Visibilities.Public ||
                symbol.isVar ||
                symbol.modality != Modality.FINAL ||
                annotation == null
            ) {
                hasIncompatible = true
            }
        }

        declaration.staticScope(context)?.processAllProperties(processor)
        declaration.symbol.resolvedCompanionObjectSymbol?.declaredMemberScope()?.processAllProperties(processor)

        if (hasIncompatible) {
            for (source in jvmFieldAnnotations) {
                reporter.reportOn(source, FirJvmErrors.INAPPLICABLE_JVM_FIELD, NOT_PUBLIC_VAL_WITH_JVMFIELD.errorMessage)
            }
        }
    }
}
