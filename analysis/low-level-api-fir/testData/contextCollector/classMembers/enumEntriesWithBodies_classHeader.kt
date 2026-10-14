package test

enum class MyEnum(val value: Int = <expr>0</expr>) {
    First(1) {
        override fun foo(): Int = value
    },
    Second {
        override fun foo(): Int = 2
    };

    abstract fun foo(): Int

    fun member(): Int = value
}
