/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

public abstract class KtJvmTypeMapper : KtAnalysisSessionComponent() {
    public abstract fun mapTypeToJvmType(type: KtType, mode: TypeMappingMode): Type
}

public interface KtJvmTypeMapperMixIn : KtAnalysisSessionMixIn {
    /**
     * Create ASM JVM type by corresponding KtType
     *
     * @see TypeMappingMode
     */
    public fun KtType.mapTypeToJvmType(mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type =
        withValidityAssertion { analysisSession.jvmTypeMapper.mapTypeToJvmType(this, mode) }
}
