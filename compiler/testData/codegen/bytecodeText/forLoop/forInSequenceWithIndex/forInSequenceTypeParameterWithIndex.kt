// IGNORE_BACKEND_FIR: JVM_IR
fun <T : Sequence<*>> test(sequence: T): String {
    val s = StringBuilder()

    for ((index, x) in sequence.withIndex()) {
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
