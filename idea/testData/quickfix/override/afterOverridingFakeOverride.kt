// "Make XX.foo open" "true"
trait X {
    fun foo()
}

trait XX : X {
    override open fun foo() {

    }
}

trait Y : X, XX {
}

class B() : Y {
    override fun foo() {
    }
}
