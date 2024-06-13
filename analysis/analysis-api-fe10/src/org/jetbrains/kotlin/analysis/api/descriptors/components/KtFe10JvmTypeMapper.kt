/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaJvmTypeMapper
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

internal class KaFe10JvmTypeMapper(
    override val analysisSession: KaFe10Session
) : KaJvmTypeMapper(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    private val typeMapper by lazy { KaFe10JvmTypeMapperContext(analysisContext.resolveSession) }

    override fun mapTypeToJvmType(type: KaType, mode: TypeMappingMode): Type {
        val kotlinType = (type as KaFe10Type).fe10Type
        return typeMapper.mapType(kotlinType, mode)
    }

    override fun isPrimitiveBacked(type: KaType): Boolean {
        val kotlinType = (type as KaFe10Type).fe10Type
        return typeMapper.isPrimitiveBacked(kotlinType)
    }
}