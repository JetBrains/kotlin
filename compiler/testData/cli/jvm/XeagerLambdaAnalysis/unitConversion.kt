fun runUnit(f: () -> Unit) {}

fun test(produce: () -> String) {
    // Passing a value of type () -> String where () -> Unit is expected requires
    // UnitConversionsOnArbitraryExpressions, postponed indefinitely (KT-87439) and NOT
    // enabled by -Xeager-lambda-analysis, so this fails to compile even with the flag.
    runUnit(produce)
}
