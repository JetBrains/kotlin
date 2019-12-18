// "Change to 'return@foo'" "false"
// ACTION: Add braces to 'if' statement
// ACTION: Change parameter 'f' type of function 'foo' to '(Int) -> Unit'
// ACTION: Change return type of called function 'baz' to 'Unit'
// ACTION: Change return type of enclosing function 'test' to 'String'
// ACTION: Convert to single-line lambda
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Move lambda argument into parentheses
// ACTION: Specify explicit lambda signature
// DISABLE-ERRORS
inline fun foo(f: (Int) -> Int) {}

fun baz(): String = ""

fun test() {
    foo { i ->
        if (i == 1) return baz()<caret>
    }
}