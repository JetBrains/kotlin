// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.enums

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.3.0
 */
@ExperimentalCompilerArgument
public enum class AssertionsMode(
  public val stringValue: String,
) {
  ALWAYS_ENABLE("always-enable"),
  ALWAYS_DISABLE("always-disable"),
  JVM("jvm"),
  LEGACY("legacy"),
  ;
}
