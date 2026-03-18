// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.buildtools.api.arguments.types

import kotlin.String
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NullabilityAnnotationMode

/**
 * @since 2.4.0
 */
@ExperimentalCompilerArgument
public class NullabilityAnnotation(
  public val annotationFqName: String,
  public val mode: NullabilityAnnotationMode,
)
