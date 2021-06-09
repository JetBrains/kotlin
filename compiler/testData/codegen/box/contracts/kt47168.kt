// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
import kotlin.contracts.*

inline fun foo(x: () -> String, y: () -> String): String {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        callsInPlace(y, InvocationKind.EXACTLY_ONCE)
    }
    return x() + y()
}

fun box(): String {
    val y = { "K" }
    return foo({ "O" }, y)
}
