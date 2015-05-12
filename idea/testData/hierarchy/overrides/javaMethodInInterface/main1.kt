interface T: A {
    override fun foo() {

    }
}

open class X: A {
    override fun foo() {

    }
}

open class Y: T {
    override fun foo() {

    }
}

open class Z: X() {
    override fun foo() {

    }
}

class SS {

}