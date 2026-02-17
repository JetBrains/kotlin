// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public enum class StringConcatMode(
  public val stringValue: String,
) {
  INDY_WITH_CONSTANTS("indy-with-constants"),
  INDY("indy"),
  INLINE("inline"),
  ;
}
