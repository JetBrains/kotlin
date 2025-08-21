/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion.Companion.CURRENTLY_SUPPORTED_VERSIONS
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.impl.AbiSignatureVersions
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler.Companion.DEFAULT_ABI_SIGNATURE_VERSION
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

@OptIn(ExperimentalLibraryAbiReader::class)
object KlibAbiDumpDirectives : SimpleDirectivesContainer() {
    val DUMP_KLIB_ABI by enumDirective<KlibAbiDumpMode>(
        description = "Enable dumping ABI of the KLIB library with a specific mode"
    )

    val KLIB_ABI_DUMP_EXCLUDED_PACKAGES by valueDirective<AbiCompoundName>(
        description = "Packages that should be filtered out from ABI dump",
        parser = ::parseCompoundName
    )

    val KLIB_ABI_DUMP_EXCLUDED_CLASSES by valueDirective<AbiQualifiedName>(
        description = "Classes that should be filtered out from ABI dump",
        parser = ::parseQualifiedName
    )

    val KLIB_ABI_DUMP_NON_PUBLIC_MARKERS by valueDirective<AbiQualifiedName>(
        description = "Non-public API markers (annotation classes)",
        parser = ::parseQualifiedName
    )

    enum class KlibAbiDumpMode(val abiSignatureVersions: Set<AbiSignatureVersion>) {
        /**
         * Generate KLIB ABI dump files for all currently supported versions of signatures.
         * See [CURRENTLY_SUPPORTED_VERSIONS] for the list of such.
         */
        ALL_SIGNATURE_VERSIONS(CURRENTLY_SUPPORTED_VERSIONS.toAbiSignatureVersions()),

        /**
         *  Generate KLIB ABI dump files only for the current "default" signature version,
         *  which corresponds to [DEFAULT_ABI_SIGNATURE_VERSION].
         */
        DEFAULT(setOf(DEFAULT_ABI_SIGNATURE_VERSION.toAbiSignatureVersion()));
    }

    private fun KotlinIrSignatureVersion.toAbiSignatureVersion() = AbiSignatureVersions.resolveByVersionNumber(number)
    private fun Collection<KotlinIrSignatureVersion>.toAbiSignatureVersions() = map { it.toAbiSignatureVersion() }.toSet()

    private fun String.removeDoubleQuotes() = removeSurrounding("\"")

    private fun parseCompoundName(value: String) = AbiCompoundName(value.removeDoubleQuotes())

    private fun parseQualifiedName(value: String): AbiQualifiedName =
        with(value.removeDoubleQuotes()) {
            AbiQualifiedName(
                packageName = AbiCompoundName(substringBefore('/', missingDelimiterValue = "")),
                relativeName = AbiCompoundName(substringAfter('/'))
            )
        }
}
