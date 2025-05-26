/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import java.io.Serializable
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.jvm.Transient
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.BOOLEAN_FALSE_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INPUT
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INTERNAL

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonToolArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public abstract class CommonToolArguments : Freezable(), Serializable {
  @Argument(
    value = "-help",
    shortName = "-h",
    description = "Print a synopsis of standard options.",
  )
  public var help: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-X",
    description = "Print a synopsis of advanced options.",
  )
  public var extraHelp: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-version",
    description = "Display the compiler version.",
  )
  public var version: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INTERNAL,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-verbose",
    description = "Enable verbose logging output.",
  )
  public var verbose: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INTERNAL,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-nowarn",
    description = "Don't generate any warnings.",
  )
  public var suppressWarnings: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-Werror",
    description = "Report an error if there are any warnings.",
  )
  public var allWarningsAsErrors: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
  )
  @Argument(
    value = "-Wextra",
    description = "Enable extra checkers for K2.",
  )
  public var extraWarnings: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  public var freeArgs: List<String> = emptyList()
    set(`value`) {
      checkFrozen()
      field = value
    }

  public var internalArguments: List<InternalArgument> = emptyList()
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Transient
  public var errors: ArgumentParseErrors? = null
}
