class B : A()

sealed class A() {
    constructor(i: Int): this()

    class C: A()
}

object T : Y()

class D : A(4)

class E : A {
    constructor(i: Int): super(i)
    constructor(): super()
}

object S : Z()

sealed class Y : X()

sealed class Z : Y()

sealed class X : A()

class Q : Y()

fun box() : String {
    B()
    A.C()
    D()
    E()
    E(4)
    T
    S
    Q()
    return "OK"
}