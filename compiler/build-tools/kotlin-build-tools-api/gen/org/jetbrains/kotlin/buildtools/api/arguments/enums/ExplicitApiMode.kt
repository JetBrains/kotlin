package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

@ExperimentalCompilerArgument
public enum class ExplicitApiMode(
  public val stringValue: String,
) {
  STRICT("strict"),
  WARNING("warning"),
  DISABLE("disable"),
  ;
}
