// FLOW: IN

open class A(val <caret>n: Int)

class B : A(1)

fun test() {
    val z = A(2).n
}