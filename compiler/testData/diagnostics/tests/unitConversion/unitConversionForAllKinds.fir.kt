// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: () -> Unit) {}
fun <T> fooGeneric(f: (T) -> Unit): T = TODO()

fun bar(): String = ""
fun createCall(): () -> Int = TODO()

fun test(g: () -> String, h: (Float) -> String) {
    foo(::bar)
    foo { "something" }
    foo(<!ARGUMENT_TYPE_MISMATCH!>g<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>fooGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>h<!>)
}
