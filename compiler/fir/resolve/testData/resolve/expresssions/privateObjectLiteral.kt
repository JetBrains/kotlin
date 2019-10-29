class C {
    private val x = object {
        fun foo() = 42
    }

    val y = x.foo()

    internal val z = object {
        fun foo() = 13
    }

    val w = z.foo() // ERROR!
}