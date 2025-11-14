// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

sealed class Result {
    object Ok : Result()
    data class Error(val code: Int) : Result()
}

fun sealedExh(r: Result): String {
    return when (r) {
        Result.Ok -> "OK"
        is Result.Error -> "ERR"
    }
}
