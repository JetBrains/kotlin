// WITH_STDLIB

class C : Iterable<String> {
    // Unused declaration, which is here only to confuse the backend who might lookup symbols by name
    private fun List<Int>.iterator(): Double = size.toDouble()

    override fun iterator(): Iterator<String> = listOf("OK").iterator()
}

fun box(): String {
    val c = C()
    for ((i, x) in c.withIndex()) {
        if (i == 0) {
            return x
        }
    }
    return "Fail"
}
