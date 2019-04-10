// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int?>) {
    list.<caret>mapNotNull {
        foo()
        it!!
    }
}

fun foo(): Int = 1