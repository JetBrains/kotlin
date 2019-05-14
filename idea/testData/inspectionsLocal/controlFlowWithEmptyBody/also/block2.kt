// PROBLEM: 'also' has empty body
// WITH_RUNTIME

fun test() {
    42.<caret>also({ })
}
