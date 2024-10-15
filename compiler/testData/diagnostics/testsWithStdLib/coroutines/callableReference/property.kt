// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// SKIP_TXT
import kotlin.coroutines.coroutineContext

val c = ::<!UNSUPPORTED!>coroutineContext<!>

fun test() {
    c()
}

suspend fun test2() {
    c()
}
