/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import kotlin.reflect.KClass

abstract class FirSamConversionTransformerExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("SamConversionTransformerExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirSamConversionTransformerExtension::class

    abstract fun getCustomFunctionTypeForSamConversion(function: FirSimpleFunction): ConeLookupTagBasedType?

    fun interface Factory : FirExtension.Factory<FirSamConversionTransformerExtension>
}

val FirExtensionService.samConversionTransformers: List<FirSamConversionTransformerExtension> by FirExtensionService.registeredExtensions()
