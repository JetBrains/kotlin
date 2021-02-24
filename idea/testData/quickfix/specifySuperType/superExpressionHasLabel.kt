// "Specify supertype" "true"
interface X

open class Y {
    open fun foo() {}
}

interface Z {
    fun foo() {}
}

class Test : X, Y(), Z {
    override fun foo() {}

    inner class Boo : Y(), Z {
        override fun foo() {
            <caret>super@Test.foo()
        }
    }
}
