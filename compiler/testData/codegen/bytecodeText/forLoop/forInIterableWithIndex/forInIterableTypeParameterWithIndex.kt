// IGNORE_BACKEND_FIR: JVM_IR
fun <T : Iterable<*>> test(iterable: T): String {
    val s = StringBuilder()

    for ((index, x) in iterable.withIndex()) {
        s.append("$index:$x;")
    }

    return s.toString()
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2
