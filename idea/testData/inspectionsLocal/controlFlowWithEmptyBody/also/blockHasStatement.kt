// PROBLEM: none
// WITH_RUNTIME

fun test(i: Int) {
    i.<caret>also {
        foo()
    }
}

fun foo() {}