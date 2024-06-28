// LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// DIAGNOSTICS: -UNUSED_VARIABLE
// FIR_IDENTICAL

inline class Foo(val x: Int)

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: Foo

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: Foo
}