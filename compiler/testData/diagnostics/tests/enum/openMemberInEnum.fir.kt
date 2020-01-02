enum class EnumWithOpenMembers {
    E1 {
        override fun foo() = 1
        override val bar: String = "a"
    },

    E2 {
        override fun f() = 3
        override val b = 4
    };

    open fun foo() = 1
    open val bar: String = ""

    fun f() = 2
    val b = 3
}