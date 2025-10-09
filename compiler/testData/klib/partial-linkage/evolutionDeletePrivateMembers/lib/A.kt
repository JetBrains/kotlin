open class X {
    private fun foo() = "private fun in superclass"
    private val bar = "private val in superclass"
    private class Z {
        fun qux() = "fun in a provate inner class in superclass"
    }
    
    fun bar() = "${foo()} $bar ${Z()}"
}

