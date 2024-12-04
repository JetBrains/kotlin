// KT-66465

inline fun <T> remember(calculation: () -> T): T = calculation()

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