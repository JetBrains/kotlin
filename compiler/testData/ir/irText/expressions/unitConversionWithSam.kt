// LANGUAGE: +UnitConversionsOnArbitraryExpressions

fun interface KRunnable {
    fun run()
}

fun useKRunnable(r: KRunnable) {}

fun test(fn: () -> String) {
    useKRunnable(fn)
}
