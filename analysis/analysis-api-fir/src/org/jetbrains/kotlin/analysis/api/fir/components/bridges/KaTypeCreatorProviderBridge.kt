/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.copy as copyEndpoint
import org.jetbrains.kotlin.analysis.api.types.typeCreation.typeCreator as typeCreatorEndpoint

@OptIn(KaExperimentalApi::class)
internal class KaTypeCreatorProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaTypeCreatorProvider {
    override val typeCreator: KaTypeCreator
        get() = context(analysisSession) { typeCreatorEndpoint }

    override fun <T : KaClassType> T.copy(init: KaClassTypeBuilder.() -> Unit): KaClassType =
        context(analysisSession) { copyEndpoint(init) }

    override fun KaUsualClassType.copy(init: KaClassTypeBuilder.() -> Unit): KaUsualClassType =
        context(analysisSession) { copyEndpoint(init) }

    override fun KaFunctionType.copy(init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType =
        context(analysisSession) { copyEndpoint(init) }

    override fun KaTypeParameterType.copy(init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType =
        context(analysisSession) { copyEndpoint(init) }
}
