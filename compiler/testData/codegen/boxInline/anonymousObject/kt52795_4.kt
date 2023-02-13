// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR

import kotlin.IllegalStateException

// FILE: 1.kt
inline fun <T> mrun(noinline block: () -> T) = block()
inline fun <T> mrunTwice(block: () -> T) : T {
    val first = block()
    val second = block()
    if (first!!::class != second!!::class)
        throw IllegalStateException("${first!!::class} != ${second!!::class}")
    return first
}

// FILE: 2.kt
fun bar(o: String): String {
    val callable = mrun {
        fun localAnonymousFun(k: String): String {
            fun localAnonymousFunLevel2() = mrunTwice {
                object {
                    fun foo() = o + k
                }
            }
            return localAnonymousFunLevel2().foo()
        }
        ::localAnonymousFun
    }

    return callable("K")
}

fun box() = bar("O")
