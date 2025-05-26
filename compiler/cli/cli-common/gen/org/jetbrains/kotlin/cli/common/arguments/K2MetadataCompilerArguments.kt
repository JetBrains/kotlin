/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import com.intellij.util.xmlb.annotations.Transient as AnnotationsTransient
import kotlin.jvm.Transient as JvmTransient

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/MetadataCompilerArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public class K2MetadataCompilerArguments : CommonCompilerArguments() {
  @Argument(
    value = "-d",
    valueDescription = "<directory|jar>",
    description = "Destination for generated .kotlin_metadata files.",
  )
  public var destination: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-classpath",
    shortName = "-cp",
    valueDescription = "<path>",
    description = "List of directories and JAR/ZIP archives to search for user .kotlin_metadata files.",
  )
  public var classpath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-module-name",
    valueDescription = "<name>",
    description = "Name of the generated .kotlin_module file.",
  )
  public var moduleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xfriend-paths",
    valueDescription = "<path>",
    description = "Paths to output directories for friend modules (modules whose internals should be visible).",
  )
  public var friendPaths: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xrefines-paths",
    valueDescription = "<path>",
    description = "Paths to output directories for refined modules (modules whose expects this module can actualize).",
  )
  public var refinesPaths: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xlegacy-metadata-jar-k2",
    description = "Produce a legacy metadata jar instead of metadata klib. Suitable only for K2 compilation",
  )
  public var legacyMetadataJar: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @get:AnnotationsTransient
  @field:JvmTransient
  override val configurator: CommonCompilerArgumentsConfigurator =
      K2MetadataCompilerArgumentsConfigurator()

  override fun copyOf(): Freezable = copyK2MetadataCompilerArguments(this, K2MetadataCompilerArguments())
}
