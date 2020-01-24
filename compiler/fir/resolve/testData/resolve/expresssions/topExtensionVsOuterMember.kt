fun Outer.Inner.foo() = 42

class Outer {

    fun foo() = ""

    inner class Inner {
        val x = foo() // Should be Int
    }
}