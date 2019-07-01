package defaultParameterValues

fun main() {
    foo(3)
}

fun foo(
    b: Int,
    //Breakpoint!
    a: Int = 5
) {
    val c = 5
}

// EXPRESSION: a
// RESULT: Parameter evaluation is not supported for '$default' methods

// EXPRESSION: b
// RESULT: Parameter evaluation is not supported for '$default' methods