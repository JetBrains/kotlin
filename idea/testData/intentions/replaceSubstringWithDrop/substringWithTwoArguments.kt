// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(s: String) {
    s.substring<caret>(0, 10)
}