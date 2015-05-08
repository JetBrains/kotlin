// "Make XX.foo open" "true"
trait X {
    fun foo()
}

trait XX : X {
    override open fun foo() {

    }
}

abstract class A(val y: XX) : X, XX by y {
}

class B(y: XX) : A(y) {
    override fun foo() {
    }
}
