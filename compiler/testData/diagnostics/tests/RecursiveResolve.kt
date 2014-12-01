class Test(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
    private val bas = bas()
    val bas2 = bas2()

    private fun bas(): Int = null!!
    private fun bas2(): Int = null!!

    fun bar() = bar(1)
    fun bar(<!UNUSED_PARAMETER!>i<!>: Int) = 2
    private fun bar2() = bar2(1)
    private fun bar2(<!UNUSED_PARAMETER!>i<!>: Int) = 2
}