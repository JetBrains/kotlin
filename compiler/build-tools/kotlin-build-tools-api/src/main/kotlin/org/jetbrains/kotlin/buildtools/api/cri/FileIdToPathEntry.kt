/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * An entry of the compiler reference index file id to path data: the mapping from a source file identifier to its path.
 *
 * Obtain instances of this interface by executing [CriToolchain.createCriFileIdToPathDataDeserializationOperation]
 * through [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation].
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface FileIdToPathEntry {
    /**
     * Identifier of a source file, as used in [LookupEntry.fileIds], or `null` if the serialized entry carries no value.
     */
    public val fileId: Int?

    /**
     * Path of the source file as the producing build system recorded it, or `null` if the serialized entry carries no value.
     * Gradle builds store it relative to the build's root project directory; compiler-generated sources may be stored absolute.
     */
    public val path: String?
}
