// IGNORE_BACKEND: JVM_IR

enum class A {
    X {
        val x = "OK"

        inner class Inner {
            fun foo() = x
        }

        val z = Inner()

        override val test = z.foo()
    };

    abstract val test: String
}

fun box() = A.X.test
