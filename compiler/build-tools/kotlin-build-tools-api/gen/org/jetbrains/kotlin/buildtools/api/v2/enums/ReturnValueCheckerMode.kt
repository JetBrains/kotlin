package org.jetbrains.kotlin.buildtools.api.v2.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.v2.ExperimentalCompilerArgument

@ExperimentalCompilerArgument
public enum class ReturnValueCheckerMode(
  public val stringValue: String,
) {
  CHECKER("check"),
  FULL("full"),
  DISABLED("disable"),
  ;
}
