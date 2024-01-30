package foo

interface foo

fun foo() {}

/**
 * [<caret_1>foo]
 *
 * [<caret_2>foo.foo]
 * [foo.<caret_3>foo]
 */
fun usage() {}