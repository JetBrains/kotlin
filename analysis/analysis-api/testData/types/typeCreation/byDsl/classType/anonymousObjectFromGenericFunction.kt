private fun <T> box(t: T) = object {
    val v = t
}

fun usage() {
    val bb = box("s")

    b<caret_type>b.toString()
}
