// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.4.20
 */
@ExperimentalCompilerArgument
public enum class DuplicatedUniqueNameStrategy(
  public val stringValue: String,
) {
  DENY("deny"),
  ALLOW_ALL_WITH_WARNING("allow-all-with-warning"),
  ALLOW_FIRST_WITH_WARNING("allow-first-with-warning"),
  ;
}
