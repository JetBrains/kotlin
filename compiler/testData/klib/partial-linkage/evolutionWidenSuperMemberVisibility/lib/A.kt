open class X {
    private fun foo1() = "private in super"
    fun testX1() = foo1()
    private fun foo2() = "private in super"
    fun testX2() = foo2()

    private val val1 = "private in super"
    fun testX3() = val1
    private val val2 = "private in super"
    fun testX4() = val2
}

