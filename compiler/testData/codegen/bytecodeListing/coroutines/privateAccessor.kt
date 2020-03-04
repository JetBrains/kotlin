// IGNORE_BACKEND: JVM_IR
// TODO: KT-37242
// WITH_RUNTIME
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*

private fun foo() {}

private suspend fun bar() = suspendCoroutine<Unit> {
    foo()
}