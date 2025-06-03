package org.jetbrains.kotlin.build.tools.api.enums

import kotlin.String

public enum class ExplicitApiMode(
  public val `value`: String,
) {
  strict("strict"),
  warning("warning"),
  disable("disable"),
  ;
}
