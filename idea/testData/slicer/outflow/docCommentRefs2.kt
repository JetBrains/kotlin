// FLOW: OUT

val String.<caret>foo: Int
    get() = 10

/**
 * Uses [foo]
 */
fun bar() {
    val v = "".foo
}
