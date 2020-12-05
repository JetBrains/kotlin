// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

value interface InlineInterface
value annotation class InlineAnn
value object InlineObject
value enum class InlineEnum
