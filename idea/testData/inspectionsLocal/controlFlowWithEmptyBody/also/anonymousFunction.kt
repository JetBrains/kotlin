// PROBLEM: 'also' has empty body
// FIX: none
// WITH_RUNTIME

fun test() {
    42.<caret>also(fun(it: Int) {
    })
}
