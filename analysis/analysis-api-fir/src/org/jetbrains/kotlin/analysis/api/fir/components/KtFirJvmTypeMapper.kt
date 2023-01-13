/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.analysis.api.components.KtJvmTypeMapper
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

internal class KtFirJvmTypeMapper(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtJvmTypeMapper(), KtFirAnalysisSessionComponent {

    override fun mapTypeToJvmType(type: KtType, mode: TypeMappingMode): Type {
        return analysisSession.useSiteSession.jvmTypeMapper.mapType(type.coneType, mode, sw = null, unresolvedQualifierRemapper = null)
    }
}
