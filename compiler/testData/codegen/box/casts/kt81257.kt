fun foo(o: Double?): String {
    var enabled: Boolean? = null
    val oE: Boolean = o!! > 0.0
    enabled = if (enabled == null) oE else enabled && oE

    return if (enabled) "OK" else "fail"
}

fun box() = foo(3.14)