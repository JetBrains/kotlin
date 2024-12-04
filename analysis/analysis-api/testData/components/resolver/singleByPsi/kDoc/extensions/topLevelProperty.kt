package test

class Foo

val Foo.extProp: Int get() = 10

/**
 * [test.<caret_1>extProp]
 * [<caret_2>extProp]
 *
 * [Foo.<caret_3>extProp]
 * [test.Foo.<caret_4>extProp]
 */
fun usage() {}