abstract class A(val x: Int)

class B : A {
    constructor(x: String) : <expr>super</expr>(x.length)
}