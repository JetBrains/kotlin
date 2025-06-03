package org.jetbrains.kotlin.buildtools.api.v2.enums

import kotlin.String

public enum class ReturnValueCheckerMode(
  public val `value`: String,
) {
  checker("check"),
  full("full"),
  disabled("disable"),
  ;
}
