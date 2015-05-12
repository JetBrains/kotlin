open class T {
    open fun <caret>foo() {

    }
}

open class X: T() {
    override fun foo() {

    }
}

open interface Y: T() {
    override fun foo() {

    }
}

open class Z: Y {
    override fun foo() {

    }
}

class SS {

}