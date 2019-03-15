/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.synthetic.hasJavaOriginInHierarchy
import org.jetbrains.kotlin.types.UnwrappedType

class JvmSamConversionTransformer(
    private val samResolver: SamConversionResolver,
    private val languageVersionSettings: LanguageVersionSettings
) : SamConversionTransformer {

    override fun getFunctionTypeForPossibleSamType(possibleSamType: UnwrappedType): UnwrappedType? =
        SingleAbstractMethodUtils.getFunctionTypeForSamType(possibleSamType, samResolver)?.unwrap()

    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean {
        if (languageVersionSettings.supportsFeature(LanguageFeature.SamConversionForKotlinFunctions)) return true

        val functionDescriptor = candidate.original as? FunctionDescriptor ?: return false
        if (functionDescriptor is TypeAliasConstructorDescriptor &&
            functionDescriptor.underlyingConstructorDescriptor is JavaClassConstructorDescriptor) return true

        return functionDescriptor.hasJavaOriginInHierarchy()
    }
}