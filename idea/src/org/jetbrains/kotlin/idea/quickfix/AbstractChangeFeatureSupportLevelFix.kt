/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.facet.KotlinFacet

abstract class AbstractChangeFeatureSupportLevelFix(
    element: PsiElement,
    protected val feature: LanguageFeature,
    protected val featureSupport: LanguageFeature.State,
    private val featureShortName: String = feature.presentableName
) : KotlinQuickFixAction<PsiElement>(element) {
    protected val featureSupportEnabled: Boolean
        get() = featureSupport == LanguageFeature.State.ENABLED || featureSupport == LanguageFeature.State.ENABLED_WITH_WARNING

    final override fun getFamilyName() = "Enable/Disable $featureShortName support"

    override fun getText(): String = getFixText(featureSupport, featureShortName)

    companion object {
        fun getFixText(state: LanguageFeature.State, featureShortName: String): String {
            return when (state) {
                LanguageFeature.State.ENABLED -> "Enable $featureShortName support"
                LanguageFeature.State.ENABLED_WITH_WARNING -> "Enable $featureShortName support (with warning)"
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> "Disable $featureShortName support"
            }
        }
    }

    abstract class FeatureSupportIntentionActionsFactory : KotlinIntentionActionsFactory() {
        protected fun shouldConfigureInProject(module: Module): Boolean {
            val facetSettings = KotlinFacet.get(module)?.configuration?.settings
            return (facetSettings == null || facetSettings.useProjectSettings) &&
                    module.getBuildSystemType() == BuildSystemType.JPS
        }

        protected fun doCreateActions(
            diagnostic: Diagnostic,
            feature: LanguageFeature,
            allowWarningAndErrorMode: Boolean,
            quickFixConstructor: (PsiElement, LanguageFeature, LanguageFeature.State) -> IntentionAction
        ): List<IntentionAction> {
            val newFeatureSupports = when (diagnostic.factory) {
                Errors.EXPERIMENTAL_FEATURE_ERROR -> {
                    if (Errors.EXPERIMENTAL_FEATURE_ERROR.cast(diagnostic).a.first != feature) return emptyList()
                    if (!allowWarningAndErrorMode) listOf(LanguageFeature.State.ENABLED)
                    else listOf(LanguageFeature.State.ENABLED_WITH_WARNING, LanguageFeature.State.ENABLED)
                }
                Errors.EXPERIMENTAL_FEATURE_WARNING -> {
                    if (Errors.EXPERIMENTAL_FEATURE_WARNING.cast(diagnostic).a.first != feature) return emptyList()
                    if (!allowWarningAndErrorMode) listOf(LanguageFeature.State.ENABLED)
                    listOf(LanguageFeature.State.ENABLED, LanguageFeature.State.ENABLED_WITH_ERROR)
                }
                else -> return emptyList()
            }

            return newFeatureSupports.map { quickFixConstructor(diagnostic.psiElement, feature, it) }
        }
    }
}
