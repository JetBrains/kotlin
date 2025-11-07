// TARGET_BACKEND: JVM_IR, WASM
// WITH_STDLIB

annotation class Inner(val i: Int = 1, val s: String = "")
annotation class Outer(
    val one: Inner,
    val many: Array<Inner>
)

private fun valueHash(v: Any): Int = when (v) {
    is BooleanArray -> v.contentHashCode()
    is ByteArray -> v.contentHashCode()
    is CharArray -> v.contentHashCode()
    is ShortArray -> v.contentHashCode()
    is IntArray -> v.contentHashCode()
    is LongArray -> v.contentHashCode()
    is FloatArray -> v.contentHashCode()
    is DoubleArray -> v.contentHashCode()
    is Array<*> -> v.contentHashCode()
    else -> v.hashCode()
}

private fun contrib(name: String, v: Any): Int =
    (127 * name.hashCode()) xor valueHash(v)

fun box(): String {
    val a1 = Outer(Inner(), arrayOf(Inner(1,"x"), Inner(2,"y")))
    val a2 = Outer(Inner(i = 1), arrayOf(Inner(1,"x"), Inner(2,"y")))
    val a3 = Outer(Inner(s = ""), arrayOf(Inner(1,"x"), Inner(2,"y")))
    val b  = Outer(Inner(i = 8), arrayOf(Inner(1,"x"), Inner(2,"y")))

    if (a1 != a2 || a1 != a3) return "Failed1"
    if (a1 == b) return "Failed2"

    val expected =
        contrib("one", a1.one) +
                contrib("many", a1.many)

    if (a1.hashCode() != expected) return "Failed3"

    return "OK"
}
