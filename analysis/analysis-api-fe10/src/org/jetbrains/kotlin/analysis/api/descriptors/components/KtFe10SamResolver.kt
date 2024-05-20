/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSamResolver
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10AnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.resolve.sam.createSamConstructorFunction
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull

internal class KaFe10SamResolver(
    override val analysisSession: KaFe10Session
) : KaSamResolver(), KaFe10AnalysisSessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun getSamConstructor(ktClassLikeSymbol: KaClassLikeSymbol): KaSamConstructorSymbol?  {
        val descriptor = getSymbolDescriptor(ktClassLikeSymbol)
        if (descriptor is ClassDescriptor && getSingleAbstractMethodOrNull(descriptor) != null) {
            val constructorDescriptor = createSamConstructorFunction(
                descriptor.containingDeclaration,
                descriptor,
                analysisContext.resolveSession.samConversionResolver,
                JvmSamConversionOracle(analysisContext.resolveSession.languageVersionSettings)
            )

            return KaFe10DescSamConstructorSymbol(constructorDescriptor, analysisContext)
        }

        return null
    }
}