// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND_K2: JVM_IR

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