// "Make XX.foo open" "true"
interface X {
    fun foo()
}

interface XX : X {
    override final fun foo() {

    }
}

abstract class A(val y: XX) : X, XX by y {
}

class B(y: XX) : A(y) {
    override<caret> fun foo() {
    }
}
/* IGNORE_FIR */
