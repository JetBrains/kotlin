open class Base {
    open fun `access$foo`(d: Derived) {}

    open fun `access$getBar$p`(d: Derived): Int = 1
    open fun `access$setBar$p`(d: Derived, i: Int) {}

    open fun `access$getBaz$p`(d: Derived): Int = 1

    open fun `access$getBoo$p`(d: Derived): Int = 1

    open fun `access$setBar1$p`(d: Derived, i: Int) {}
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
