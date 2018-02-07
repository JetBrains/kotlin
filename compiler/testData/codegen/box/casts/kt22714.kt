fun <T> Array<T>.getLength(): Int {
    return this.size
}

fun Any.getLength() =
    if (this is Array<*>) size else -1

fun box(): String {
    val array1: Array<String> = arrayOf("1", "2", "3")
    val array2: Any = arrayOf("1", "2", "3")
    if (array1.getLength() + array2.getLength() != 6)
        return "FAILURE"
    return "OK"
}