// "Change parameter 'z' type of function 'foo' to '(Int) -> String'" "false"

// ACTION: Disable 'Move Lambda Function Into Parentheses'
// ACTION: Edit intention settings
// ACTION: Move lambda function into parentheses

fun foo(y: Int = 0, z: (Int) -> String = {""}) {
    foo {
        ""<caret> as Int
        ""
    }
}