// IS_APPLICABLE: false
// WITH_RUNTIME

const val x = 0

fun foo(s: String) {
    s.substring<caret>(x, 10)
}