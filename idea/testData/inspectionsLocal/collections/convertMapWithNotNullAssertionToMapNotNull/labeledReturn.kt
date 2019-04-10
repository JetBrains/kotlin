// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>map {
        foo()
        return@map it!!
    }
}

fun foo(): Int = 1