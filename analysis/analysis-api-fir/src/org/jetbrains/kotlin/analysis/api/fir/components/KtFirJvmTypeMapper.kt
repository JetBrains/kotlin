/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.analysis.api.components.KaJvmTypeMapper
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

internal class KaFirJvmTypeMapper(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaJvmTypeMapper(), KaFirSessionComponent {

    override fun mapTypeToJvmType(type: KaType, mode: TypeMappingMode): Type {
        return analysisSession.useSiteSession.jvmTypeMapper.mapType(type.coneType, mode, sw = null, unresolvedQualifierRemapper = null)
    }

    override fun isPrimitiveBacked(type: KaType): Boolean {
        return analysisSession.useSiteSession.jvmTypeMapper.isPrimitiveBacked(type.coneType)
    }
}
