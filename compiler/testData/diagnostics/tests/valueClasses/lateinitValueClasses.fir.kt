// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

lateinit var a: Foo

fun foo() {
    lateinit var b: Foo
}