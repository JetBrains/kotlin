package test

enum class MyEnum(val value: Int = 0) {
    First(<expr>1</expr>) {
        override fun foo(): Int = value
    },
    Second {
        override fun foo(): Int = 2
    };

    abstract fun foo(): Int

    fun member(): Int = value
}
