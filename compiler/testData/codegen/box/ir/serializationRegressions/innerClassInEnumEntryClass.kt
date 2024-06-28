// JVM_ABI_K1_K2_DIFF: KT-62714, KT-63880

// MODULE: lib
// FILE: lib.kt
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

// MODULE: main(lib)
// FILE: main.kt
fun box() = A.X.test
