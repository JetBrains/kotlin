// IGNORE_BACKEND: JVM
// WITH_STDLIB

operator fun ClosedRange<Double>.iterator() =
    object : Iterator<Double> {
        private var current = this@iterator.start
        private val end = this@iterator.endInclusive

        override fun hasNext(): Boolean =
            current <= end

        override fun next(): Double {
            val next = current
            current += 0.1
            return next
        }
    }

fun box(): String {
    var s = 0.0
    for (x in 0.0 .. 1.0) {
        s += x
    }
    if (s != 5.5)
        return "Failed: $s"

    return "OK"
}
