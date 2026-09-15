// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM
// WITH_STDLIB

import kotlin.jvm.JvmInline

@JvmInline
value class ValueClassJvm<caret>Inline(val value: Int)
