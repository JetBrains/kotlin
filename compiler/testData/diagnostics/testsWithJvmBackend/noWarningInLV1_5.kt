// !LANGUAGE: +JvmInlineValueClasses
// WITH_RUNTIME

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class VC(val a: Any)