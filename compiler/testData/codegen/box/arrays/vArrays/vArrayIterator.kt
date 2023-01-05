// ENABLE_JVM_IR_INLINER

fun box(): String {
    val arr = VArray(3) { it + 1 }
    val stringBuilder = StringBuilder()
    val iterator = arr.iterator()
    while (iterator.hasNext()) {
        stringBuilder.append(iterator.next())
    }

    if (stringBuilder.toString() != "123") return "Fail"

    return "OK"
}