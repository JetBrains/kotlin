// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

suspend fun escapeChar(c : Char) : String? = when (c) {
    '\\' -> "\\\\"
    '\n' -> "\\n"
    '"'  -> "\\\""
    else -> "" + c
}

tailrec suspend fun String.escape(i : Int = 0, result : StringBuilder = StringBuilder()) : String =
        if (i == length) result.toString()
        else escape(i + 1, result.append(escapeChar(get(i))))

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var res = ""
    builder {
        res = "test me not \\".escape()
    }
    return if (res == "test me not \\\\") "OK" else res
}
