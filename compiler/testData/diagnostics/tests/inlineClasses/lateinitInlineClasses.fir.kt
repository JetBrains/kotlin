// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int)

lateinit var a: Foo

fun foo() {
    lateinit var b: Foo
}