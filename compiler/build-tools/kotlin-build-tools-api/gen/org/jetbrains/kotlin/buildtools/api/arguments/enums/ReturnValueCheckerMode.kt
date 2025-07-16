package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

@ExperimentalCompilerArgument
public enum class ReturnValueCheckerMode(
  public val stringValue: String,
) {
  CHECKER("check"),
  FULL("full"),
  DISABLED("disable"),
  ;
}
