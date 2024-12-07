// SKIP_UNBOUND_IR_SERIALIZATION
// ^^^ Muted: Class `S` is private. Function `S.a()` is effectively private. However, both are used in
//     `internal inline fun S.call2()` which is a compilation warning since 2.1.0 (see KT-69681)
//     and should become a compilation error in 2.1.20 (see KT-70916).

// FILE: 1.kt

package test

private class S {
    fun a(): String {
        return "K"
    }

    // This function exposes S which is a private class (package-private in the byte code)
    // It can be accessed outside the `test` package, which may lead to IllegalAccessError.
    // This behavior may be changed later
    internal inline fun call(s: S.() -> String): String {
        return call2(s)
    }
}

@Suppress(
    "IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR",
    "PRIVATE_CLASS_MEMBER_FROM_INLINE",
    "EXPOSED_PARAMETER_TYPE",
    "EXPOSED_RECEIVER_TYPE",
)
internal inline fun S.call2(s: S.() -> String): String {
    return s() + a()
}

internal fun call(): String {
    return S().call {
        "O"
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return call()
}
