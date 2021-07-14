fun <R> runCatching(block: () -> R) = null <!CAST_NEVER_SUCCEEDS!>as<!> Result<R>

class Result<out T> {
    fun getOrNull(): T? = null
}

fun main() {
    runCatching {
        null
    }.getOrNull() // don't report IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION
}
