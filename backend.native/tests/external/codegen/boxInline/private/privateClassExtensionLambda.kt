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

@Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE", "EXPOSED_PARAMETER_TYPE", "EXPOSED_RECEIVER_TYPE")
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
