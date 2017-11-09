// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun <E : Enum<E>> Byte.toEnum(clazz : Class<E>) : E =
    (clazz.getMethod("values").invoke(null) as Array<E>)[this.toInt()]

enum class Letters { A, B, C }

fun box(): String {
    val clazz = Letters::class.java
    val r = 1.toByte().toEnum(clazz)
    return if (r == Letters.B) "OK" else "Fail: $r"
}
