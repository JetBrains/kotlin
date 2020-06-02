// !LANGUAGE: -SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: () -> Unit) {}

fun bar(): Int = 0

abstract class SubInt : () -> Int

fun test(f: () -> String, s: SubInt) {
    foo { "lambda" }
    foo(::bar)

    foo(<!UNSUPPORTED_FEATURE!>f<!>)
    foo(<!UNSUPPORTED_FEATURE!>s<!>)
}