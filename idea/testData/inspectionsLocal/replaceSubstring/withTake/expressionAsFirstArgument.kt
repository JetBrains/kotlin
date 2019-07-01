// PROBLEM: none
// WITH_RUNTIME

fun foo(s: String) {
    s.substring<caret>(1 - 1, 10)
}