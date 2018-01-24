// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// FILE: 1.kt
package test

import kotlin.internal.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> myrun(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// FILE: 2.kt
import test.*

fun box(): String {
    val x: Long
    myrun {
        x = 42L
    }
    return if (x.inc() == 43L) "OK" else "Fail: ${x.inc()}"
}