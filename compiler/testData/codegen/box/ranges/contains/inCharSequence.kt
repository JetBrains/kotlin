// WITH_RUNTIME

val charSeq: String = "123"

fun box(): String = when {
    '0' in charSeq -> "fail 1"
    '1' !in charSeq -> "fail 2"
    else -> "OK"
}