/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * An entry of the compiler reference index subtype data: the direct subtypes of one supertype.
 *
 * Obtain instances of this interface by executing [CriToolchain.createCriSubtypeDataDeserializationOperation]
 * through [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation].
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface SubtypeEntry {
    /**
     * Hash code of the supertype's fully qualified name, or `null` if the serialized entry carries no value.
     */
    public val fqNameHashCode: Int?

    /**
     * Fully qualified names of the supertype's direct subtypes.
     */
    public val subtypes: Collection<String>
}
