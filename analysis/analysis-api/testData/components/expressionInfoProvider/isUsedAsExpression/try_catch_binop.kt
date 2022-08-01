import java.lang.Exception

fun test() {
    <caret>try { throw Exception() } finally { return } + 4
    return
}