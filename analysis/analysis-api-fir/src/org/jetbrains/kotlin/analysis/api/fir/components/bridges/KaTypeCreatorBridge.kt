/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaArrayTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeCreator
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.name.ClassId

@OptIn(KaExperimentalApi::class)
internal class KaTypeCreatorBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaTypeCreator {
    private val proxy: KaInternalsTypeCreator
        get() = analysisSession.legacyTypeCreator

    override fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit): KaType =
        proxy.buildClassType(classId, init)

    override fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit): KaType =
        proxy.buildClassType(symbol, init)

    override fun buildArrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit): KaType =
        proxy.buildArrayType(elementType, init)

    override fun buildVarargArrayType(elementType: KaType): KaType =
        proxy.buildVarargArrayType(elementType)

    override fun buildTypeParameterType(
        symbol: KaTypeParameterSymbol,
        init: KaTypeParameterTypeBuilder.() -> Unit,
    ): KaTypeParameterType = proxy.buildTypeParameterType(symbol, init)

    override fun buildStarTypeProjection(): KaStarTypeProjection = proxy.buildStarTypeProjection()
}
