/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/MetadataCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

class K2MetadataCompilerArguments : CommonCompilerArguments() {
    @Argument(
        value = "-Xfriend-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for friend modules (modules whose internals should be visible).",
    )
    var friendPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlegacy-metadata-jar-k2",
        description = "Produce a legacy metadata jar instead of metadata klib. Suitable only for K2 compilation",
    )
    var legacyMetadataJar: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xrefines-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for refined modules (modules whose expects this module can actualize).",
    )
    var refinesPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "List of directories and JAR/ZIP archives to search for user .kotlin_metadata files.",
    )
    var classpath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-d",
        valueDescription = "<directory|jar>",
        description = "Destination for generated .kotlin_metadata files.",
    )
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-module-name",
        valueDescription = "<name>",
        description = "Name of the generated .kotlin_module file.",
    )
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    @field:kotlin.jvm.Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2MetadataCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyK2MetadataCompilerArguments(this, K2MetadataCompilerArguments())
}
