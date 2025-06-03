package org.jetbrains.kotlin.build.tools.api.enums

import kotlin.String

public enum class ReturnValueCheckerMode(
  public val `value`: String,
) {
  checker("check"),
  full("full"),
  disabled("disable"),
  ;
}
