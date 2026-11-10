/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.cri

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import java.nio.file.Path

@Serializable
internal class CompilerRefIndexConfigurationImpl(
    override val outputDirectory: Path,
) : CompilerRefIndexConfiguration, CompilerRefIndexConfiguration.Builder, DeepCopyable<CompilerRefIndexConfigurationImpl> {

    private constructor(other: CompilerRefIndexConfigurationImpl) : this(other.outputDirectory)

    override fun build(): CompilerRefIndexConfiguration = deepCopy()

    override fun toBuilder(): CompilerRefIndexConfiguration.Builder = deepCopy()

    override fun deepCopy(): CompilerRefIndexConfigurationImpl = CompilerRefIndexConfigurationImpl(this)

    @UseFromImplModuleRestricted
    override fun <V> get(key: CompilerRefIndexConfiguration.Option<V>): V = throwPropertyNotFound(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: CompilerRefIndexConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        throwPropertyNotFound(key.id)
    }
}
