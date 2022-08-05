import java.lang.Exception

fun test(b: Boolean) {
    throw if (b) { <expr>Exception()</expr> } else { Exception() }
}