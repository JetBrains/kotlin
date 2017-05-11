// IS_APPLICABLE: false
// ERROR: 'if' must have both main and 'else' branches if used as an expression
// ERROR: Type mismatch: inferred type is Unit but Int was expected

fun maybeFoo(): String? {
    return "foo"
}

fun bar(): Int {
    val foo = maybeFoo()
    return if (foo != null<caret>) {
        foo.length
    }
}
