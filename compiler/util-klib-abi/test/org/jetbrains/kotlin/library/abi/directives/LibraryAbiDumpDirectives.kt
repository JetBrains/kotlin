/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.directives

import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

@OptIn(ExperimentalLibraryAbiReader::class)
object LibraryAbiDumpDirectives : SimpleDirectivesContainer() {
    val EXCLUDED_PACKAGES by valueDirective<AbiCompoundName>(
        description = "Packages that should be filtered out from ABI dump",
        parser = ::parseCompoundName
    )

    val EXCLUDED_CLASSES by valueDirective<AbiQualifiedName>(
        description = "Classes that should be filtered out from ABI dump",
        parser = ::parseQualifiedName
    )

    val NON_PUBLIC_MARKERS by valueDirective<AbiQualifiedName>(
        description = "Non-public API markers (annotation classes)",
        parser = ::parseQualifiedName
    )

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
