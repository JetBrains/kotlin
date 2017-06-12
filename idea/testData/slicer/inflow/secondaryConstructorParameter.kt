// FLOW: IN

open class A {
    constructor(<caret>n: Int, s: String = "???")
}

class B1: A(1)
class B2: A(1, "2")
class B3: A(1, s = "2")
class B4: A(n = 1, s = "2")
class B5: A(s = "2", n = 1)

fun test() {
    A(1)
    A(1, "2")
    A(1, s = "2")
    A(n = 1, s = "2")
    A(s = "2", n = 1)
}