// !SKIP_JAVAC
// !API_VERSION: 1.4
// !LANGUAGE: -InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.jvm

annotation class JvmInline

value class Foo(val x: Int)

value annotation class InlineAnn
value object InlineObject
value enum class InlineEnum

@JvmInline
value class NotVal(<!INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>x: Int<!>)
