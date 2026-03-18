// TARGET_BACKEND: JVM
// WITH_STDLIB

operator fun String.iterator(): IntIterator = object : IntIterator() {
    private var index = 0

    override fun nextInt() = codePointAt(index).apply {
        index += Character.charCount(this)
    }

    override fun hasNext(): Boolean = index < length
}

fun String.collectInts(): List<Int> {
    val result = ArrayList<Int>()
    for (c in this) {
        result.add(c)
    }
    return result
}

fun box(): String {
    val ints = String(Character.toChars(127849)).collectInts()
    return if (ints == listOf(127849)) "OK" else "Fail: $ints"
}