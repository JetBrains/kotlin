// FLOW: OUT
// WITH_DEREFERENCES

class A {
    operator fun get(i: Int) = this
    operator fun set(i: Int, a: A) = this
    operator fun plusAssign(a: A) = this
    operator fun times(a: A) = this
    operator fun inc() = this
}

fun test() {
    val <caret>x = A()
    val y = A()

    x[1]
    x[1] = y
    x[1] += y
    x[1] *= y
    x[1]++
}