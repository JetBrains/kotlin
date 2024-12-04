// TARGET_BACKEND: JVM_IR

fun box(): String {
    val a = Any()
    val a1 = true === a
    val a2 = false === a
    val a3 = 1.toInt() === a
    val a4 = 1.toLong() === a
    val a5 = 1.toShort() === a
    val a6 = 1.toByte() === a
    val a7 = 1.toChar() === a
    val a8 = 1.toFloat() === a
    val a9 = 1.toDouble() === a
    return if (!a1 && !a2 && !a3 && !a4 && !a5 && !a6 && !a7 && !a8 && !a9) {
        "OK"
    } else {
        "Fail"
    }
}
