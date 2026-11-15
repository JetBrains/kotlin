// TARGET_BACKEND: WASM

fun byteDispatch(value: Byte): Int = when (value) {
    -3 -> 1
    5 -> 2
    7 -> 3
    else -> 4
}

fun shortDispatch(value: Short): Int = when (value) {
    -300 -> 1
    500 -> 2
    700 -> 3
    else -> 4
}

fun denseIntDispatch(value: Int): Int = when (value) {
    10 -> 1
    11 -> 2
    12 -> 3
    else -> 4
}

fun longDispatch(value: Long): Int = when (value) {
    Long.MIN_VALUE -> 1
    -3L -> 2
    5L -> 3
    Long.MAX_VALUE -> 4
    else -> 5
}

fun box(): String {
    if (byteDispatch((-3).toByte()) != 1 || byteDispatch(6) != 4) return "Byte"
    if (shortDispatch((-300).toShort()) != 1 || shortDispatch(600) != 4) return "Short"
    if (denseIntDispatch(11) != 2 || denseIntDispatch(13) != 4) return "Dense table"
    if (longDispatch(Long.MIN_VALUE) != 1 || longDispatch(Long.MAX_VALUE) != 4) return "Long boundary"
    if (longDispatch(4L) != 5 || longDispatch(-3L) != 2) return "Long fallback"
    return "OK"
}
