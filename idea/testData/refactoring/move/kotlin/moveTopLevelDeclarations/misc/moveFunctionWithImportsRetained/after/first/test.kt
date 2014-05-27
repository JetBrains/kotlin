package first

import second.A
import third.B
import third.D
import fourth.X

fun test() {
    val a = A()
    val b = B()
    val d_ = D()
    val c = B.C()
    val x = X()
    val y = X.Y()
}