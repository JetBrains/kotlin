// JVM_TARGET: 1.8

fun box(): String {
    val a = BooleanWrap(false)
    return if (a < true) "OK" else "Fail"
}

class BooleanWrap(private val value: Boolean): Comparable<Boolean> by value
