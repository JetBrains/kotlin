// FILE: test.kt
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

const val DEBUG = false
inline fun inlineFun(b: () -> Unit) {
    if (DEBUG) {
        inlineFunReal(b)
    }
}

inline fun inlineFunReal(b: () -> Unit) {
    try {
        b()
    } finally {
    }
}

// FILE: box.kt
fun builder(c: suspend () -> Unit) {}

class Sample {
    fun test() {
        inlineFun {
            builder {
                inlineFun {
                    suspendFun()
                }
            }
        }
    }

    suspend fun suspendFun() {}
}

fun box(): String {
    Sample().test()
    return "OK"
}