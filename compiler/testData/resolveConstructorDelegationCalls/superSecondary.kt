open class B {
    constructor(x: Int) {}
}
trait C
class A : B, C {
    <caret>constructor(): super(1) { }
}
