// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>map {
        it!!
        foo()
    }
}

fun foo(): Int = 1