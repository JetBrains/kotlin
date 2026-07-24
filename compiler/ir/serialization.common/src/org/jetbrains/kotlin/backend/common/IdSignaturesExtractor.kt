/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * This is an interface for a lightweight tool that allows extracting [IdSignature]s from the given [KotlinLibrary].
 *
 * There are two entry points:
 * - [extractAllPublicSignatures] extracts signatures of both top-level and nested/member public declarations.
 *   This endpoint is useful for running investigations, debugging, etc.
 * - [extractOnlyTopLevelPublicSignatures] extracts signatures of only top-level public declarations.
 *   This endpoint can be helpful for e.g. understanding the DAG of dependencies between libraries.
 *
 * Note: [extractOnlyTopLevelPublicSignatures] is written in way to do even lesser amount of IO reads. So, it's
 * supposed to be more robust than [extractAllPublicSignatures].
 */
interface IdSignaturesExtractor {
    /**
     * The two sets of extracted signatures:
     *
     * @property declaredSignatures The signatures of public declarations that belong to the current library.
     * @property importedSignatures The signatures of public declarations that belong to other libraries,
     *   but are referenced/called in the current library. I.e. "imports".
     */
    data class ExtractedSignatures(
        val declaredSignatures: Set<IdSignature>,
        val importedSignatures: Set<IdSignature>,
    )

    /**
     * Extracts signatures of both top-level and nested/member public declarations.
     *
     * Note: This endpoint is useful when we need to see all contents of a KLIB.
     * E.g. for running investigations, debugging, etc.
     */
    fun extractAllPublicSignatures(): ExtractedSignatures

    /**
     * Extracts signatures of only top-level public declarations.
     *
     * Note: This endpoint can be helpful for e.g. understanding the DAG of dependencies between libraries.
     * Also, it is written in way to do even lesser amount of IO reads than [extractAllPublicSignatures] for even better performance.
     */
    fun extractOnlyTopLevelPublicSignatures(): ExtractedSignatures
}
