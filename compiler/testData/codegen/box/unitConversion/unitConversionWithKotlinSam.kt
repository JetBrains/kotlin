// IGNORE_BACKEND_K2: ANY
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393

fun interface KRunnable {
    fun run()
}

fun execute(r: KRunnable) {
    r.run()
}

var sideEffect = ""

fun test(g: () -> String) {
    execute(g)
}

fun box(): String {
    test { sideEffect += "OK"; "ignored" }
    return sideEffect
}
