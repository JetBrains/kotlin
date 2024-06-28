/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeAttribute
import kotlin.reflect.KClass

/**
 * Note that when you declare your own attribute you HAVE TO declare accessor to it using following syntax:
 *
 * class MyAttribute : ConeAttribute<MyAttribute>() {...}
 *
 * val ConeAttributes.myAttribute: MyAttribute? by ConeAttributes.attributeAccessor<MyAttribute>()
 */
abstract class FirTypeAttributeExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("AdditionalTypeAttributeExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension>
        get() = FirTypeAttributeExtension::class

    abstract fun extractAttributeFromAnnotation(annotation: FirAnnotation): ConeAttribute<*>?

    /**
     * Please don't convert attributes which you didn't create
     * If [attribute] came from compiler or another plugin just return null
     */
    abstract fun convertAttributeToAnnotation(attribute: ConeAttribute<*>): FirAnnotation?

    fun interface Factory : FirExtension.Factory<FirTypeAttributeExtension>
}

val FirExtensionService.typeAttributeExtensions: List<FirTypeAttributeExtension> by FirExtensionService.registeredExtensions()
