package test

trait X {
    fun foo()
}

trait Y : X {
}

class B(val a: X) : X by a, Y {
    override fun foo() {
    }
}
