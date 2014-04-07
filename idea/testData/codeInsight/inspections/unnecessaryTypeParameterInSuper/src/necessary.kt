trait X {
    fun foo() {
    }
}

trait Y {
    fun foo() {
    }
}

class NecessaryTypeParameter() : X, Y {
    override fun foo() {
        super<X>.foo() // shouldn't warn
        super<Y>.foo() // shouldn't warn
    }
}
