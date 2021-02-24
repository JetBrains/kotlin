/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.types.KotlinType

class JvmSamConversionOracle(
    private val languageVersionSettings: LanguageVersionSettings
) : SamConversionOracle {

    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean {
        if (languageVersionSettings.supportsFeature(LanguageFeature.SamConversionForKotlinFunctions)) return true
        return JavaBasedSamConversionOracle.shouldRunSamConversionForFunction(candidate)
    }

    override fun isPossibleSamType(samType: KotlinType): Boolean =
        JavaBasedSamConversionOracle.isPossibleSamType(samType)

    override fun isJavaApplicableCandidate(candidate: CallableDescriptor): Boolean =
        JavaBasedSamConversionOracle.shouldRunSamConversionForFunction(candidate)
}