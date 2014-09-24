open class Base {
    open fun `foo$b$0`(d: Derived) {}

    open fun `getBar$b$1`(d: Derived): Int = 1
    open fun `setBar$b$1`(d: Derived, i: Int) {}

    open fun `getBaz$b$2`(d: Derived): Int = 1

    open fun `getBoo$b$3`(d: Derived): Int = 1

    open fun `setBar1$b$4`(d: Derived, i: Int) {}
}

class Derived : Base() {
    private fun foo() {}

    private var bar = 1
        get
        set

    private var baz = 1

    private val boo = 1

    private var bar1 = 1
        get
        set

    inner class Nested {
        fun test() {
            foo()
            bar += 1
            baz += 1
            val s = boo
            bar1 += 1
        }
    }
}
