// NO_CHECK_LAMBDA_INLINING
// KT-66465

// FILE: lib.kt
inline fun <T> remember(calculation: () -> T): T = calculation()

// FILE: main.kt
fun box(): String {
    val result = remember {
        object {
            fun foo(): String {
                val a = remember {
                    object {
                        fun foo() = "OK"
                    }
                }

                return a.foo()
            }
        }
    }

    return result.foo()
}