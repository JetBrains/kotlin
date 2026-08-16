/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DefaultImportsProvider
import org.jetbrains.kotlin.resolve.ImportPath

object JsDefaultImportsProvider : DefaultImportsProvider() {

    private const val KOTLIN_JS_PACKAGE = "kotlin.js"

    override val platformSpecificDefaultImports: List<ImportPath> =
        listOf(
            ImportPath.fromString("$KOTLIN_JS_PACKAGE.*")
        )

    /**
     * Names from kotlin.js that should not be imported through
     * the platform-specific wildcard import.
     */
    private val excludedNames: Set<String> = setOf(
        "Promise",
        "Date",
        "Console",
        "Math",
        "RegExp",
        "RegExpMatch",
        "Json",
        "json"
    )

    override val excludedImports: List<FqName> =
        excludedNames.map { "$KOTLIN_JS_PACKAGE.$it" }
            .map(::FqName)

    /**
     * Returns true when the given fully qualified name is excluded
     * from the Kotlin/JS default imports.
     */
    fun isExcludedImport(fqName: FqName): Boolean =
        fqName in excludedImports.toSet()

    /**
     * Returns true when the given simple name is excluded from
     * the Kotlin/JS default imports.
     */
    fun isExcludedName(name: String): Boolean =
        name in excludedNames

    /**
     * Creates an import path for a Kotlin/JS package.
     *
     * This is useful for future platform-specific default imports
     * without duplicating ImportPath construction.
     */
    fun importPackage(packageName: String): ImportPath =
        ImportPath.fromString(packageName)

    /**
     * Returns all Kotlin/JS excluded names.
     *
     * A copy is returned so callers cannot modify the internal set.
     */
    fun getExcludedNames(): Set<String> =
        excludedNames.toSet()
}
