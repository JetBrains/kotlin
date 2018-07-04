// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// IGNORE_BACKEND: JVM_IR
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
    val x: Int
    myrun {
        x = 42
    }
    return if (x.inc() == 43) "OK" else "Fail: ${x.inc()}"
}