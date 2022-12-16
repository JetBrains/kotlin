/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtJvmTypeMapper
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KtFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

internal class KtFe10JvmTypeMapper(
    override val analysisSession: KtFe10AnalysisSession
) : KtJvmTypeMapper(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    private val typeMapper by lazy { KtFe10JvmTypeMapperContext(analysisContext.resolveSession) }

    override fun mapTypeToJvmType(type: KtType, mode: TypeMappingMode): Type {
        val kotlinType = (type as KtFe10Type).fe10Type
        return typeMapper.mapType(kotlinType, mode)
    }
}