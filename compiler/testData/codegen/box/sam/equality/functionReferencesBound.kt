// TARGET_BACKEND: JVM
// FILE: test.kt

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

private fun id(f: Runnable): Any = f

class C {
    fun target1() {}
    fun target2() {}
    
    fun adapted1(s: String? = null): String = s!!
    fun adapted2(vararg s: String): String = s[0]
}

fun box(): String {
    // Since 1.0, SAM wrappers for Java do not implement equals/hashCode
    val c0 = C()

    checkNotEqual(id(c0::target1), id(c0::target1))
    checkNotEqual(id(c0::target1), target1FromOtherFile(c0))
    checkNotEqual(id(c0::target1), id(c0::target2))

    checkNotEqual(id(c0::adapted1), id(c0::adapted1))
    checkNotEqual(id(c0::adapted1), adapted1FromOtherFile(c0))
    checkNotEqual(id(c0::adapted2), id(c0::adapted2))
    checkNotEqual(id(c0::adapted2), adapted2FromOtherFile(c0))
    checkNotEqual(id(c0::adapted1), id(c0::adapted2))

    val c1 = C()
    checkNotEqual(id(c0::target1), id(c1::target1))
    checkNotEqual(id(c0::target1), id(c1::target2))
    checkNotEqual(id(c0::adapted1), id(c1::adapted1))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun id(f: Runnable): Any = f

fun target1FromOtherFile(c0: C): Any = id(c0::target1)
fun adapted1FromOtherFile(c0: C): Any = id(c0::adapted1)
fun adapted2FromOtherFile(c0: C): Any = id(c0::adapted2)
