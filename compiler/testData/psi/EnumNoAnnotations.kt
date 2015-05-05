package a.b.c.test.enum

enum class Enum {
    // We have six entries in the following line,
    // not one entry with five annotations
    A B C D E F {
        override fun f() = 4
    }

    open fun f() = 3
}