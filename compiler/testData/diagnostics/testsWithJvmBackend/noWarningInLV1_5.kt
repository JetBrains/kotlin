// !LANGUAGE: +JvmInlineValueClasses
// WITH_STDLIB

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class VC(val a: Any)