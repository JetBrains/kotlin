// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>map { i ->
        foo()
        i!!
    }
}

fun foo(): Int = 1