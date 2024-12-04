// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
package test

@Suppress("NULLABLE_INLINE_PARAMETER")
inline fun build(func: () -> Unit, pathFunc: (() -> String)? = null) {
    func()

    pathFunc?.invoke()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    build ({ result = "OK" })

    return result
}
