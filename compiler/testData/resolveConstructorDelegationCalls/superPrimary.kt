open class B(x: Int)
interface C
class A : B, C {
    <caret>constructor(): super(1) { }
}
