class Outer {

    fun foo() = 1

    inner class Inner {

        val x = this@Outer.foo()

        val y = foo()
    }
}