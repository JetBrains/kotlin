// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>map other@{
        foo()
        return@other it!!
    }
}

fun foo(): Int = 1