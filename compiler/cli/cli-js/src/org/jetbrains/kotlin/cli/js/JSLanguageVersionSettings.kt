/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.config.*

class JSLanguageVersionSettings(private val delegate: LanguageVersionSettings) : LanguageVersionSettings {
    companion object {
        private val disabledFeatures = setOf(
            LanguageFeature.NewInference,
            LanguageFeature.FunctionalInterfaceConversion,
            LanguageFeature.SamConversionForKotlinFunctions,
            LanguageFeature.SamConversionPerArgument,
            LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType,
            LanguageFeature.NonStrictOnlyInputTypesChecks
        )
    }

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        return if (feature in disabledFeatures)
            LanguageFeature.State.DISABLED
        else
            delegate.getFeatureSupport(feature)
    }

    override fun isPreRelease(): Boolean = delegate.isPreRelease()

    override fun <T> getFlag(flag: AnalysisFlag<T>): T = delegate.getFlag(flag)

    override val apiVersion: ApiVersion
        get() = delegate.apiVersion

    override val languageVersion: LanguageVersion
        get() = delegate.languageVersion
}