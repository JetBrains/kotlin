class Test {
    val Long.foo: Long
        get() = this + 1

    val Int.foo: Int
        get() = this + 1

    fun testLong(): Long? {
        var s: Long? = 10;
        return s?.foo
    }

    fun testInt(): Int? {
        var s: Int? = 11;
        return s?.foo
    }
}

fun box(): String {
    val s = Test()

    if (s.testLong() != 11.toLong()) return "fail 1"

    if (s.testInt() != 12) return "fail 1"

    return "OK"
}