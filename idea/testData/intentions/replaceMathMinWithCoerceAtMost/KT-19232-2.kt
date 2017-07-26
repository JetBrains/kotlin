// WITH_RUNTIME

fun foo() {
    Pair(Math.min(1, 3)<caret>, Math.min(2, 4)).let { println(it) }
}