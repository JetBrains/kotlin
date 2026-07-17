// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.5.0
 */
@ExperimentalCompilerArgument
public enum class ValhallaSupportMode(
  public val stringValue: String,
) {
  NONE("none"),
  PRIMITIVES("primitives"),
  PRIMITIVES_AND_FULL_VALUE_CLASSES("primitivesAndFullValueClasses"),
  ALL_VALUES("allValues"),
  ;
}
