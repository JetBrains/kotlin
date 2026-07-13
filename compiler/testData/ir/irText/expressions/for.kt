// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM invokes `println (message: kotlin.Int)` instead of `println (message: kotlin.Any?)`

fun testEmpty(ss: List<String>) {
    for (s in ss);
}

fun testIterable(ss: List<String>) {
    for (s in ss) {
        println(s)
    }
}

fun testDestructuring(pp: List<Pair<Int, String>>) {
    for ([i, s] in pp) {
        println(i)
        println(s)
    }
}

fun testRange() {
    for (i in 1..10);
}
