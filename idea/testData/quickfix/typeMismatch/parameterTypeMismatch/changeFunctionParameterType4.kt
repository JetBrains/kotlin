// "Change parameter 'z' type of function 'foo' to '(Int) -> String'" "false"
// ACTION: To raw string literal

fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo {
        ""<caret> as Int
        ""
    }
}