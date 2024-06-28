enum class EnumClass {
    E1 {
        override fun foo() = 1
        override val bar: String = "a"
    },

    <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>E2<!> {

    };

    abstract fun foo(): Int
    abstract val bar: String
}