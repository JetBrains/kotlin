package test

class Foo

fun Foo.extFun() {}

/**
 * [test.<caret_1>extFun]
 * [<caret_2>extFun]
 *
 * [Foo.<caret_3>extFun]
 * [test.Foo.<caret_4>extFun]
 */
fun usage() {}