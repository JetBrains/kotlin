// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun some() {}

suspend fun test() {
    try {
        some()
    } finally {
        some()
    }
}