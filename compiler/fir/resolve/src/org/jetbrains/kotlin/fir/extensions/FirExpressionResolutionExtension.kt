/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.reflect.KClass

abstract class FirExpressionResolutionExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("ExpressionResolutionExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirExpressionResolutionExtension::class

    abstract fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType>

    fun interface Factory : FirExtension.Factory<FirExpressionResolutionExtension>
}

val FirExtensionService.expressionResolutionExtensions: List<FirExpressionResolutionExtension> by FirExtensionService.registeredExtensions()
