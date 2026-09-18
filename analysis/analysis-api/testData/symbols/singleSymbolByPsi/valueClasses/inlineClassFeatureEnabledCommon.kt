// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: Common
// WITH_STDLIB

import kotlin.jvm.JvmInline

@JvmInline
value class ValueClassJvmInline<caret>Common(val value: Int)
