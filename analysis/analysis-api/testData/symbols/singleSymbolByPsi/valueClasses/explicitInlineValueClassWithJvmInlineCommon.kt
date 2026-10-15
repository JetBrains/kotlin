// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: Common
// WITH_STDLIB

import kotlin.jvm.JvmInline

@JvmInline
inline value class ExplicitInline<caret>ValueClass(val value: Int)
