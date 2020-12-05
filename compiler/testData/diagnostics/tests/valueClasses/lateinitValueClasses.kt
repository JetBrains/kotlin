// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: Foo

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: Foo
}