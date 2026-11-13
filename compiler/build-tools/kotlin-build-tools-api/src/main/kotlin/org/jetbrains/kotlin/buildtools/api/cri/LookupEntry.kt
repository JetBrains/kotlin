/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * An entry of the compiler reference index lookups data: the source files that reference one symbol.
 *
 * Obtain instances of this interface by executing [CriToolchain.createCriLookupDataDeserializationOperation]
 * through [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation].
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface LookupEntry {
    /**
     * Hash code of the referenced symbol's fully qualified name, or `null` if the serialized entry carries no value.
     */
    public val fqNameHashCode: Int?

    /**
     * Identifiers of the source files that reference the symbol.
     * Every identifier resolves to a file path through a [FileIdToPathEntry] of the same index.
     */
    public val fileIds: Collection<Int>
}
