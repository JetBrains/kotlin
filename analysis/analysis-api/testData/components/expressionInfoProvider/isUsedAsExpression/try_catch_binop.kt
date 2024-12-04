import java.lang.Exception

fun test() {
    <expr>try { throw Exception() } finally { return }</expr> + 4
    return
}