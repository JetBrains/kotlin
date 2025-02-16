/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirContextParametersLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirContextParametersDeclarationChecker.checkSubTypes
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.contextParameterTypes
import org.jetbrains.kotlin.fir.types.hasContextParameters
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

object FirContextualFunctionTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Platform) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef.source?.kind is KtFakeSourceElementKind) return
        if (!typeRef.coneType.abbreviatedTypeOrSelf.hasContextParameters) return

        val source = typeRef.source?.findContextReceiverListSource()
            ?: errorWithAttachment("Source for type ref of contextual function type doesn't contain context list.") {
                withFirEntry("fir", typeRef)
            }

        source.forEachChildOfType(valueParameterElementSet, depth = 1) {
            reporter.reportOn(it, FirErrors.NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE, context)
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            if (checkSubTypes(typeRef.coneType.contextParameterTypes(context.session), context)) {
                reporter.reportOn(
                    source,
                    FirErrors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS,
                    context
                )
            }
            val message = FirContextParametersLanguageVersionSettingsChecker.getMessage(context.languageVersionSettings)
            reporter.reportOn(typeRef.source, FirErrors.CONTEXT_RECEIVERS_DEPRECATED, message, context)
        } else if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
            reporter.reportOn(
                source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.ContextParameters to context.languageVersionSettings,
                context
            )
        }
    }

    private val valueParameterElementSet = setOf(KtStubElementTypes.VALUE_PARAMETER)

    private fun KtSourceElement.findContextReceiverListSource(): KtSourceElement? {
        fun PsiElement.findContextReceiverListSource(): KtPsiSourceElement? {
            return when (this) {
                is KtTypeReference -> typeElement?.findContextReceiverListSource()
                is KtNullableType -> innerType?.findContextReceiverListSource()
                is KtFunctionType -> contextReceiverList?.toKtPsiSourceElement()
                else -> null
            }
        }

        fun LighterASTNode.findContextReceiverListSource(tree: FlyweightCapableTreeStructure<LighterASTNode>): KtLightSourceElement? {
            return when (tokenType) {
                KtNodeTypes.TYPE_REFERENCE, KtNodeTypes.NULLABLE_TYPE -> tree
                    .findChildByType(this, KtTokenSets.TYPE_ELEMENT_TYPES)
                    ?.findContextReceiverListSource(tree)
                KtNodeTypes.FUNCTION_TYPE -> tree
                    .findChildByType(this, KtNodeTypes.CONTEXT_RECEIVER_LIST)
                    ?.toKtLightSourceElement(tree)
                else -> null
            }
        }

        return when (this) {
            is KtPsiSourceElement -> psi.findContextReceiverListSource()
            is KtLightSourceElement -> lighterASTNode.findContextReceiverListSource(treeStructure)
        }
    }
}

