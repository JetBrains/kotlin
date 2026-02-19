// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals
