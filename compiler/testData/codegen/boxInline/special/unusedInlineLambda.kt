// FILE: 1.kt
package test

/*inline fun <R> rruunn(block: () -> R): R {
    contract {
        callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return block()
}*/

inline fun h(): Int {
    return run {
        1
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    h()
    return "OK"
}
