/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.JvmFieldApplicabilityProblem.*
import org.jetbrains.kotlin.name.JvmNames.JVM_MULTIFILE_CLASS_SHORT
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingDeclarationSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_MULTIFILE_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmFieldApplicabilityChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID) ?: return
        val session = context.session
        val containingClassSymbol = declaration.containingClass()?.toFirRegularClassSymbol(session)

        val problem = when {
            declaration.delegate != null -> DELEGATE
            !declaration.hasBackingField -> return
            declaration.isOverridable(containingClassSymbol) -> NOT_FINAL
            Visibilities.isPrivate(declaration.visibility) -> PRIVATE
            declaration.hasCustomAccessor() -> CUSTOM_ACCESSOR
            declaration.isOverride -> OVERRIDES
            declaration.isLateInit -> LATEINIT
            declaration.isConst -> CONST
            containingClassSymbol != null && containingClassSymbol.isInsideCompanionObjectOfInterface(session) -> {
                if (!session.languageVersionSettings.supportsFeature(LanguageFeature.JvmFieldInInterface)) {
                    INSIDE_COMPANION_OF_INTERFACE
                } else {
                    if (!isInterfaceCompanionWithPublicJvmFieldProperties(containingClassSymbol)) {
                        NOT_PUBLIC_VAL_WITH_JVMFIELD
                    } else {
                        return
                    }
                }
            }
            containingClassSymbol == null && isInsideJvmMultifileClassFile(context) ->
                TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE
            declaration.returnTypeRef.isInlineClassThatRequiresMangling(session) -> RETURN_TYPE_IS_INLINE_CLASS
            else -> return
        }

        val factory = if (declaration.fromPrimaryConstructor == true &&
            !context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor)
        ) {
            FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING
        } else {
            FirJvmErrors.INAPPLICABLE_JVM_FIELD
        }

        reporter.reportOn(annotation.source, factory, problem.errorMessage, context)
    }

    private fun FirTypeRef.isInlineClassThatRequiresMangling(session: FirSession): Boolean {
        val symbol = this.coneType.toRegularClassSymbol(session) ?: return false
        return symbol.isInline && !symbol.isDontMangleClass()
    }

    private fun FirRegularClassSymbol.isDontMangleClass(): Boolean {
        return this.classId == StandardClassIds.Result
    }

    private fun FirProperty.isOverridable(containingClass: FirRegularClassSymbol?): Boolean {
        return visibility != Visibilities.Private && modality != Modality.FINAL &&
                containingClass?.isFinal != true
    }

    private fun FirProperty.hasCustomAccessor(): Boolean {
        return (getter != null && getter?.source?.kind !is FirFakeSourceElementKind) ||
                (setter != null && setter?.source?.kind !is FirFakeSourceElementKind)
    }

    private fun FirRegularClassSymbol.isInsideCompanionObjectOfInterface(session: FirSession): Boolean {
        if (!isCompanion) {
            return false
        }

        val outerClassKind = getContainingDeclarationSymbol(session)?.classKind
        return outerClassKind == ClassKind.INTERFACE || outerClassKind == ClassKind.ANNOTATION_CLASS
    }

    private fun isInterfaceCompanionWithPublicJvmFieldProperties(containingClass: FirRegularClassSymbol): Boolean {
        for (symbol in containingClass.declarationSymbols) {
            if (symbol !is FirPropertySymbol) continue

            if (symbol.visibility != Visibilities.Public || symbol.isVar || symbol.modality != Modality.FINAL) {
                return false
            }

            if (!symbol.hasJvmFieldAnnotation()) {
                return false
            }
        }

        return true
    }

    private fun FirPropertySymbol.hasJvmFieldAnnotation(): Boolean {
        return getAnnotationByClassId(JVM_FIELD_ANNOTATION_CLASS_ID) != null
    }

    private fun isInsideJvmMultifileClassFile(context: CheckerContext): Boolean {
        return context.findClosest<FirFile>()?.annotations?.any {
            it.annotationTypeRef.coneType.classId == JVM_MULTIFILE_CLASS_ID
        } == true
    }
}
