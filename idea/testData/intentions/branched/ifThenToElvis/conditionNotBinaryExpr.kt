// IS_APPLICABLE: false
// ERROR: Type mismatch: inferred type is Int but Boolean was expected
// ERROR: Type mismatch: inferred type is Int but Boolean was expected
// ERROR: Operator call corresponds to a dot-qualified call 'foo.times(10)' which is not allowed on a nullable receiver 'foo'.

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
