fun box(): String {
    return justPrint(9.compareTo(4))
} 

fun justPrint(value: Int): String {
    return if (value > 0) "OK" else "Fail $value"
}
