package one.two

class Foo

fun Foo.ext() {}

/**
 * [<caret_1>one.two.ext]
 * [one.<caret_2>two.ext]
 *
 * [<caret_3>Foo.ext]
 * [one.two.<caret_4>Foo.ext]
 *
 * [<caret_5>one.two.Foo.ext]
 * [one.<caret_6>two.Foo.ext]
 */
fun usage() {}