// IGNORE_BACKEND: JVM_IR

enum class A {
    X {
        val x = "OK"

        inner class Inner {
            inner class Inner2 {
                inner class Inner3 {
                    val y = x
                }
            }
        }

        val z = Inner().Inner2().Inner3()

        override val test: String
            get() = z.y
    };

    abstract val test: String
}

fun box() = A.X.test
