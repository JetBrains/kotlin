// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public enum class KlibIrInlinerMode(
  public val stringValue: String,
) {
  INTRAMODULE("intra-module"),
  FULL("full"),
  DISABLED("disabled"),
  DEFAULT("default"),
  ;
}
