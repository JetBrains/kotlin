// NO_CHECK_LAMBDA_INLINING

import kotlin.IllegalStateException

// FILE: 1.kt
inline fun <T> mrun2(noinline block: () -> T, block2: () -> Unit): T { block2(); return block() }

// FILE: 2.kt
fun bar(o: String): String {
    val obj = mrun2(
        {
            object {
                fun foo() = o + "K"
            }
        },
        {
            fun localFun() = 42
        }
    )

    return obj.foo()
}

fun box() = bar("O")