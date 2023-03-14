// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

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
