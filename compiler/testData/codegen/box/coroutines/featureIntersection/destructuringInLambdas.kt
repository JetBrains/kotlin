// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82376
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

data class A(val o: String) {
    operator fun component2(): String = "K"
}
fun builder(c: suspend (A) -> Unit) {
    (c as (suspend A.() -> Unit)).startCoroutine(A("O"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        [x, y] ->
        result = x + y
    }

    return result
}
