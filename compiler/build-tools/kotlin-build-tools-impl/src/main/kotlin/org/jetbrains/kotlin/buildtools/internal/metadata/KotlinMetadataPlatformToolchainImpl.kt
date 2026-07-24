/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.metadata

import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.internal.metadata.operations.KotlinMetadataKlibCompilationOperationImpl
import java.nio.file.Path

internal class KotlinMetadataPlatformToolchainImpl(private val compilerVersion: String) : KotlinMetadataPlatformToolchain {
    override fun metadataKlibCompilationOperationBuilder(
        sources: List<Path>,
        destination: Path,
    ): KotlinMetadataKlibCompilationOperation.Builder =
        KotlinMetadataKlibCompilationOperationImpl(
            sources,
            destination,
            compilerVersion = compilerVersion
        )
}
