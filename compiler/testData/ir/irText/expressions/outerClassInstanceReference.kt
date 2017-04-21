class Outer {
    fun outer() {}

    inner class Inner {
        fun inner() = outer()
    }
}