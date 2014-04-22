package first

import util.*

fun test() {
    topLevelFun(topLevelVar)
    topLevelFun(topLevelVal)
    val c = C("ff", 1)
    c.s.<error>invalid</error>
    val <warning>b</warning> = B()
    funWithVararg(1, 2, 3)
    val <warning>i</warning> = Invalid()
    topLevelObject.f()
    topLevelObject.g()
 }

fun testWhere(list: List<Int>) {
    funWithWhere(1, list)
    <error>funWithWhere</error>(1, 2)
}

class Example : C("foo", 0)
class Example2 : <error>A</error>(2)
