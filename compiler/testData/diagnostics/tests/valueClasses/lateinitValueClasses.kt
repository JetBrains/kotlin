// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: Foo

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: Foo
}