// JVM_ABI_K1_K2_DIFF: KT-62714, KT-63880

enum class A {
    X {
        val x = "OK"

        inner class Inner {
            fun foo() = this@X.x
        }

        val z = Inner()

        override val test = z.foo()
    };

    abstract val test: String
}

fun box() = A.X.test
