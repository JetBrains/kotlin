// FIR_IDENTICAL
package a.b.c.test.enum

enum class Enum {
    A, B, C, D, E, F {
        override fun f() = 4
    };

    open fun f() = 3

    companion object {
        @Ann
        val c: Int = 1
    }
}

annotation class Ann
