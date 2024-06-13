// LANGUAGE: -UnitConversionsOnArbitraryExpressions
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: () -> Unit) {}

fun bar(): Int = 0

abstract class SubInt : () -> Int

fun <T> T.freeze(): T = TODO()

fun test(f: () -> String, g: () -> Nothing, h: () -> Nothing?, s: SubInt) {
    foo { "lambda" }
    foo(::bar)
    foo({ TODO() }.freeze())
    foo(g)

    foo(<!UNSUPPORTED_FEATURE!>h<!>)

    foo(<!UNSUPPORTED_FEATURE!>f<!>)
    foo(<!UNSUPPORTED_FEATURE!>s<!>)
}