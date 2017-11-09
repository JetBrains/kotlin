// FLOW: OUT
// WITH_DEREFERENCES

class A {
    operator fun plus(n: Int) = this
    operator fun unaryPlus() = this
    operator fun inc() = this
    operator fun timesAssign(n: Int) = this
}

operator fun A.minus(n: Int) = this
operator fun A.unaryMinus() = this
operator fun A.dec() = this
operator fun A.divAssign(n: Int) = this

fun test() {
    var <caret>x = A()

    +x
    x + 1
    x++
    x += 1
    x *= 1

    -x
    x - 1
    x--
    x -= 1
    x /= 1
}