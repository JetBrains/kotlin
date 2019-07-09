// PROBLEM: 'also' has empty body
// FIX: none
// WITH_RUNTIME

fun test(i: Int) {
    i.<caret>also {
        // comment
    }
}