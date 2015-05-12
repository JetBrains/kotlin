open class B(x: Double) {
    constructor(x: Int) {}
    constructor(x: String) {}
}
interface C
class A : B, C {
    <caret>constructor(): super("abc") { }
}
