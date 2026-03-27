// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393

fun useUnit(fn: () -> Unit) {}

fun useSuspendUnit(fn: suspend () -> Unit) {}

fun produceFun(): () -> String = { "" }

fun testSimple(fn: () -> String) {
    useUnit(fn)
}

fun testNonVal() {
    useUnit(produceFun())
}

fun testCombinedSuspendUnit(fn: () -> Int) {
    useSuspendUnit(fn)
}
