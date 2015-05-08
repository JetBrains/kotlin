// "Make XX.foo open" "true"
trait X {
    fun foo()
}

trait XX : X {
    override final fun foo() {

    }
}

trait Y : X, XX {
}

class B() : Y {
    override<caret> fun foo() {
    }
}
