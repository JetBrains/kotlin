// WITH_STDLIB

object ExtProvider {
    operator fun Long.get(i: Int) = this
    operator fun Long.set(i: Int, newValue: Long) {}
}

fun box(): String {
    with (ExtProvider) {
        var x = 0L
        val y = x[0]++
        return if (y == 0L) "OK" else "Failed, y=$y"
    }
}
