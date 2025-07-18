package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public enum class ReturnValueCheckerMode(
  public val stringValue: String,
) {
  CHECKER("check"),
  FULL("full"),
  DISABLED("disable"),
  ;
}
