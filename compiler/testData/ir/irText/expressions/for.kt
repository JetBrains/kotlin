// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// IGNORE_BACKEND: NATIVE

fun testEmpty(ss: List<String>) {
    for (s in ss);
}

fun testIterable(ss: List<String>) {
    for (s in ss) {
        println(s)
    }
}

fun testDestructuring(pp: List<Pair<Int, String>>) {
    for ((i, s) in pp) {
        println(i)
        println(s)
    }
}

fun testRange() {
    for (i in 1..10);
}
