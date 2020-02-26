// "Specify supertype" "true"
interface X {
    fun foo() {}
}

open class Y: X {
    override fun foo() {}
}

interface Z {
    fun foo() {}
}

class Test : X, Y(), Z {
    override fun foo() {
        <caret>super.foo()
    }
}