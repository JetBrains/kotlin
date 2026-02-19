// WITH_STDLIB
// TARGET_BACKEND: JVM
// API_VERSION: LATEST
// PREFER_IN_TEST_OVER_STDLIB

// FILE: Spilling.kt

package kotlin.coroutines.jvm.internal

@Suppress("UNUSED_PARAMETER", "unused")
internal fun nullOutSpilledVariable(value: Any?): Any? = value

// FILE: test.kt


import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class Test {
    private suspend fun startConfiguration() {
        suspendCoroutineUninterceptedOrReturn<String> { uCont ->
        }

        foo()
    }

    private suspend fun foo() {}
}

fun box(): String {
    Test()
    return "OK"
}