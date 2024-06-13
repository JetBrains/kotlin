/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

public abstract class KaJvmTypeMapper : KaSessionComponent() {
    public abstract fun mapTypeToJvmType(type: KaType, mode: TypeMappingMode): Type
    public abstract fun isPrimitiveBacked(type: KaType): Boolean
}

public typealias KtJvmTypeMapper = KaJvmTypeMapper

public interface KaJvmTypeMapperMixIn : KaSessionMixIn {
    /**
     * Create ASM JVM type by corresponding KaType
     *
     * @see TypeMappingMode
     */
    public fun KaType.mapTypeToJvmType(mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type =
        withValidityAssertion { analysisSession.jvmTypeMapper.mapTypeToJvmType(this, mode) }

    /**
     * Returns true if the type is backed by a single JVM primitive type
     */
    public val KaType.isPrimitiveBacked: Boolean
        get() = withValidityAssertion { analysisSession.jvmTypeMapper.isPrimitiveBacked(this) }
}

public typealias KtJvmTypeMapperMixIn = KaJvmTypeMapperMixIn