// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo(s: String) {
    s.substring<caret>(3, s.length - 5)
}