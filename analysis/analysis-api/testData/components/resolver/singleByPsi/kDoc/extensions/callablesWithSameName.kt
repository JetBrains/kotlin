package test

class Foo

fun Foo.ext() {}

val Foo.ext: Int get() = 10

/**
 * [test.<caret_1>ext]
 * [<caret_2>ext]
 *
 * [Foo.<caret_3>ext]
 * [test.Foo.<caret_4>ext]
 */
fun usage() {}