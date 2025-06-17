package org.jetbrains.kotlin.buildtools.api.v2.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.v2.ExperimentalCompilerArgument

@ExperimentalCompilerArgument
public enum class ExplicitApiMode(
  public val stringValue: String,
) {
  STRICT("strict"),
  WARNING("warning"),
  DISABLE("disable"),
  ;
}
