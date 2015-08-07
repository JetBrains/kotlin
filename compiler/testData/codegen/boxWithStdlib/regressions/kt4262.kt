fun <E : Enum<E>> Byte.toEnum(clazz : Class<E>) : E =
    (clazz.getMethod("values").invoke(null) as Array<E>)[this.toInt()]

enum class Letters { A, B, C }

fun box(): String {
    val clazz = javaClass<Letters>()
    val r = 1.toByte().toEnum(clazz)
    return if (r == Letters.B) "OK" else "Fail: $r"
}
