// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.types

import java.nio.`file`.Path
import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument

/**
 * @since 2.4.0
 */
@ExperimentalCompilerArgument
public class ProfileCompilerCommand(
  public val profilerPath: Path,
  public val command: String,
  public val outputDir: Path,
)
