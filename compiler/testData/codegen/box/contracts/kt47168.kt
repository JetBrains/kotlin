// OPT_IN: kotlin.contracts.ExperimentalContracts

// FILE: lib.kt
import kotlin.contracts.*

inline fun foo(x: () -> String, y: () -> String): String {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        callsInPlace(y, InvocationKind.EXACTLY_ONCE)
    }
    return x() + y()
}

// FILE: main.kt
fun box(): String {
    val y = { "K" }
    return foo({ "O" }, y)
}
