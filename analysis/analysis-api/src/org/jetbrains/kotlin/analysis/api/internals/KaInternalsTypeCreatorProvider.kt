/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsTypeCreatorProvider {
    @KaExperimentalApi
    public val typeCreator: KaTypeCreator

    @KaExperimentalApi
    public fun <T : KaClassType> copy(type: T, init: KaClassTypeBuilder.() -> Unit): KaClassType

    @KaExperimentalApi
    public fun copy(type: KaUsualClassType, init: KaClassTypeBuilder.() -> Unit): KaUsualClassType

    @KaExperimentalApi
    public fun copy(type: KaFunctionType, init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType

    @KaExperimentalApi
    public fun copy(type: KaTypeParameterType, init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType
}
