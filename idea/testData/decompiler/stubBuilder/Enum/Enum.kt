package a.b.c.test.enum

enum class Enum {
    A B C D E F {
        override fun f() = 4
    }

    open fun f() = 3

    default object {
        val c: Int = 1
    }
}