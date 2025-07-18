package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public enum class ExplicitApiMode(
  public val stringValue: String,
) {
  STRICT("strict"),
  WARNING("warning"),
  DISABLE("disable"),
  ;
}
