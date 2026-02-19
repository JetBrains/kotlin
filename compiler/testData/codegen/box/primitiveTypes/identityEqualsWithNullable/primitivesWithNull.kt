// TARGET_BACKEND: JVM_IR

fun getNull(): Any? = null

fun box(): String {
    val a1 = true === getNull()
    val a2 = false === getNull()
    val a3 = 1.toInt() === getNull()
    val a4 = 1.toLong() === getNull()
    val a5 = 1.toShort() === getNull()
    val a6 = 1.toByte() === getNull()
    val a7 = 1.toChar() === getNull()
    val a8 = 1.toFloat() === getNull()
    val a9 = 1.toDouble() === getNull()
    return if (!a1 && !a2 && !a3 && !a4 && !a5 && !a6 && !a7 && !a8 && !a9) {
        "OK"
    } else {
        "Fail"
    }
}
