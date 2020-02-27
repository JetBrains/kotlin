// IGNORE_BACKEND: JVM_IR
// TODO: KT-37084
// WITH_RUNTIME
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun some() {}

suspend fun test() {
    try {
        some()
    } finally {
        some()
    }
}