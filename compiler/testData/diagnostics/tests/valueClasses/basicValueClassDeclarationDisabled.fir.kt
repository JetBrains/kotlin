// !LANGUAGE: -InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.jvm

annotation class JvmInline

value class Foo(val x: Int)

value annotation class InlineAnn
value object InlineObject
value enum class InlineEnum

@JvmInline
value class NotVal(x: Int)