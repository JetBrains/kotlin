// !LANGUAGE: +UnitConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: () -> Unit) {}
fun <T> fooGeneric(f: (T) -> Unit): T = TODO()

fun bar(): String = ""
fun createCall(): () -> Int = TODO()

fun test(g: () -> String, h: (Float) -> String) {
    foo(::bar)
    foo { "something" }
    foo(g)

    fooGeneric(h)
}
