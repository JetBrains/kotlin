package a

import b.*

class A

class X(val y: Y? = null)
class Z : Y()

fun topLevelA1() {
    topLevelB()
}

fun topLevelA2() {}
