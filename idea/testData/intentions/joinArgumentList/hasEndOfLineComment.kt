// IS_APPLICABLE: false
fun foo(
    a: Int, // comment
    b: Int
) = 1

val x = foo(
    <caret>1, // comment
    2
)