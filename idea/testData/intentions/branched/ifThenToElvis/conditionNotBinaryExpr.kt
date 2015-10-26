// IS_APPLICABLE: false
// ERROR: Type mismatch: inferred type is kotlin.Int but kotlin.Boolean was expected
// ERROR: Condition must be of type kotlin.Boolean, but is of type kotlin.Int
// ERROR: Infix call corresponds to a dot-qualified call 'foo.times(10)' which is not allowed on a nullable receiver 'foo'. Use ?.-qualified call instead

fun main(args: Array<String>) {
    val foo: Int? = 4
    val bar = 3
    if (foo * 10<caret>) {
        foo
    }
    else {
        bar
    }
}
