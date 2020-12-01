// SKIP_TXT
import kotlin.coroutines.coroutineContext

val c = ::coroutineContext

fun test() {
    c()
}

suspend fun test2() {
    c()
}
