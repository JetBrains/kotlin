// "Change parameter 'z' type of function 'foo' to '(Int) -> String'" "false"

// ACTION: Edit intention settings

fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo {
        ""<caret> as Int
        ""
    }
}