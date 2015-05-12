open class B {
    constructor(x: Int) {}
}
interface C
class A : B, C {
    <caret>constructor(): super(1) { }
}
