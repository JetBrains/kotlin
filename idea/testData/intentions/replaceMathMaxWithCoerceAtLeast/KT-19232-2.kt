// WITH_RUNTIME

fun foo() {
    Pair(Math.max(1, 3)<caret>, Math.max(2, 4)).let { println(it) }
}