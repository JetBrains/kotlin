// WITH_STDLIB
// ISSUE: KT-32462

fun decodeValue(value: String): Any {
    return when (value[0]) {
        'F' -> String::toFloat
        'B' -> String::toBoolean
        'I' -> String::toInt
        else -> throw IllegalArgumentException("Unexpected value prefix: ${value[0]}")
    }(value.substring(2))
}

fun box(): String = "OK"
