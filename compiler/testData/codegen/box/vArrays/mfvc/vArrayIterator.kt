// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER
// TARGET_FRONTEND: FIR
// LANGUAGE: +ValueClasses

@JvmInline
value class Point(val x: Int, val y: Int)

inline fun <reified T> throws(block: () -> Unit): Boolean {
    try {
        block.invoke()
    } catch (t: Throwable) {
        return t is T
    }
    return false
}

fun box(): String {

    val arr = VArray(2) { Point(it, it + 1) }
    val it = arr.iterator()

    if (!it.hasNext()) return "Fail 1"
    if (it.next().toString() != "Point(x=0, y=1)") return "Fail 2"
    if (!it.hasNext()) return "Fail 3"
    if (it.next().toString() != "Point(x=1, y=2)") return "Fail 4"
    if (it.hasNext()) return "Fail 5"
    if (!throws<NoSuchElementException> { it.next() }) return "Fail 6"
    if (it.hasNext()) return "Fail 6"
    if (!throws<NoSuchElementException> { it.next() }) return "Fail 7"

    // TODO: fix and check exception messages

    return "OK"
}