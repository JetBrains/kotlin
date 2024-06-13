package test

class Foo

fun nonExtFun() {}
val nonExtProp: Int = 10

/**
 * [Foo.<caret_1>nonExtFun]
 * [test.Foo.<caret_2>nonExtFun]
 *
 * [Foo.<caret_3>nonExtProp]
 * [test.Foo.<caret_4>nonExtProp]
 */
fun usage() {}