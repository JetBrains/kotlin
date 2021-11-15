// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun some() {}

suspend fun test() {
    try {
        some()
    } finally {
        some()
    }
}
