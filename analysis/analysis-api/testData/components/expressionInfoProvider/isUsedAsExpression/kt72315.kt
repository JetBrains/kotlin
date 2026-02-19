// LANGUAGE: +WhenGuards
// IGNORE_FE10

sealed class Status
object Loading : Status()
data class Ok(val info: List<String>) : Status()
data class Error(val isCritical: Boolean) : Status()


fun checkStatus(status: Status): String = when (status) {
    is Error -> TODO()
    Loading -> TODO()
    is Ok if <expr> status.info.isEmpty() || true </expr> -> "No data or error"
    is Ok -> TODO()
}