abstract class A(val x: Int)

class B : A {
    constructor(x: String) : super(<expr>x.length</expr>)
}