// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
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