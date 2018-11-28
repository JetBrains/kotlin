// IGNORE_BACKEND: JVM_IR
// LANGUAGE_VERSION: 1.2

enum class A {
    X {
        val x = "OK"

        inner class Inner {
            val y = x
        }

        val z = Inner()

        override val test: String
            get() = z.y
    };

    abstract val test: String
}

fun box() = A.X.test