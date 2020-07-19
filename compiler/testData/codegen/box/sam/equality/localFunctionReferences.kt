// TARGET_BACKEND: JVM

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

private fun id(f: Runnable): Any = f

fun box(): String {
    // Since 1.0, SAM wrappers for Java do not implement equals/hashCode

    fun local1() {}
    fun local2() {}

    checkNotEqual(id(::local1), id(::local1))
    checkNotEqual(id(::local1), id(::local2))

    fun String.localExt() {}

    checkNotEqual(id("A"::localExt), id("A"::localExt))
    checkNotEqual(id("A"::localExt), id("B"::localExt))

    fun adapted(default: String? = "", vararg va: Int): Int = 0

    checkNotEqual(id(::adapted), id(::adapted))

    return "OK"
}
