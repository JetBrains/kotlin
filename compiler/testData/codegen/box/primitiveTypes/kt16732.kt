// KJS_WITH_FULL_RUNTIME
//WITH_RUNTIME

fun valueFromDB(value: Any): Any {
    return when (value) {
        is Char -> value
        is Number-> value.toChar()
        is String -> value.single()
        else -> error("Unexpected value of type Char: $value")
    }
}

fun box(): String {
    valueFromDB(1)
    return "" + valueFromDB("O") + valueFromDB("K")
}