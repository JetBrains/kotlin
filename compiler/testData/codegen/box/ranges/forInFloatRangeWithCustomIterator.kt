// IGNORE_BACKEND: JVM
// WITH_STDLIB

operator fun ClosedRange<Float>.iterator() =
    object : Iterator<Float> {
        private var current = this@iterator.start
        private val end = this@iterator.endInclusive

        override fun hasNext(): Boolean =
            current <= end

        override fun next(): Float {
            val next = current
            current += 0.125f
            return next
        }
    }

fun box(): String {
    var s = 0.0
    for (x in 0.0f .. 1.0f) {
        s += x
    }
    if (s != 4.5)
        return "Failed: $s"

    return "OK"
}
