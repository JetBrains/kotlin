// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
package test

import kotlin.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> myrun(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}


// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val x: Int
    val res = myrun {
        x = 42
        {
            x
        }()
    }
    return if (res == 42 && x.inc() == 43) "OK" else "Fail: ${x.inc()}"
}