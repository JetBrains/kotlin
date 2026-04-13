// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393

fun foo(f: () -> Unit) {
    f()
}

var sideEffect = ""

fun bar(): String {
    sideEffect += "OK"
    return "ignored"
}

fun test(g: () -> String) {
    foo(g)
}

fun box(): String {
    test(::bar)
    return sideEffect
}
