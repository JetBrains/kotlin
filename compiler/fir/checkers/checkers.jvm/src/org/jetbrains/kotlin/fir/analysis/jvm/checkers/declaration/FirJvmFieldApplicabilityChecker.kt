/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.JvmFieldApplicabilityProblem.*
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.config.LanguageFeature.ForbidJvmAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.isInlineClassThatRequiresMangling
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
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
            containingClassSymbol != null && containingClassSymbol.isInterfaceOrInterfaceCompanion(session) ->
                if (!containingClassSymbol.isInterfaceWithPublicJvmFieldProperties()) {
                    NOT_PUBLIC_VAL_WITH_JVMFIELD
                } else {
                    return
                }
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

    private fun FirRegularClassSymbol.isInterfaceOrInterfaceCompanion(session: FirSession): Boolean {
        if (isInterface) return true
        if (!isCompanion) {
            return false
        }

        val outerClassKind = getContainingDeclaration(session)?.classKind
        return outerClassKind == ClassKind.INTERFACE || outerClassKind == ClassKind.ANNOTATION_CLASS
    }

    context(context: CheckerContext)
    private fun FirRegularClassSymbol.isInterfaceWithPublicJvmFieldProperties(): Boolean {
        var result = true
        val processor = processAllDeclaredCallables@ { symbol: FirCallableSymbol<*> ->
            if (!result || symbol !is FirPropertySymbol) return@processAllDeclaredCallables

            if (symbol.visibility != Visibilities.Public ||
                symbol.isVar ||
                symbol.modality != Modality.FINAL ||
                !symbol.hasJvmFieldAnnotation(context.session)
            ) {
                result = false
            }
        }

        if (isInterface) {
            staticScope(context)?.processAllProperties(processor)
            resolvedCompanionObjectSymbol?.declaredMemberScope()?.processAllProperties(processor)
        } else {
            declaredMemberScope().processAllProperties(processor)
            (getContainingDeclaration(context.session) as? FirRegularClassSymbol)?.staticScope(context)?.processAllProperties(processor)
        }

        return result
    }

    private fun FirPropertySymbol.hasJvmFieldAnnotation(session: FirSession): Boolean {
        return backingFieldSymbol?.hasAnnotationWithClassId(JVM_FIELD_ANNOTATION_CLASS_ID, session) == true
    }

    context(context: CheckerContext)
    private fun isInsideJvmMultifileClassFile(): Boolean {
        return context.containingFileSymbol?.hasAnnotation(JVM_MULTIFILE_CLASS_ID, context.session) == true
    }
}
