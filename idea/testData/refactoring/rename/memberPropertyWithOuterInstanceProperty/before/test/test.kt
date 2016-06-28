package test

class X {
    val x: Int get() = 1

    inner class A {
        val /*rename*/a: Int get() = 1

        inner class XX {
            val xx: Int get() = 1

            fun test() {
                a
            }
        }
    }
}