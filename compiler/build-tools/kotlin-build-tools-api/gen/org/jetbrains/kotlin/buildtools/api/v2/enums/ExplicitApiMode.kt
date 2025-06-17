package org.jetbrains.kotlin.buildtools.api.v2.enums

import kotlin.String

public enum class ExplicitApiMode(
  public val stringValue: String,
) {
  strict("strict"),
  warning("warning"),
  disable("disable"),
  ;
}
