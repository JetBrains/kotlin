interface AssertDSL {
    infix fun Any?.equals(other: Any?) {
        if (this != other) throw AssertionError("$this != $other")
    }

    fun Any?.toString(): String = ""
    fun Any?.hashCode(): Int = 0
}

class C(val x: Int) : AssertDSL

fun C.test(): String {
    x equals 42
    null.toString()
    null.hashCode()
    return "OK"
}

fun box(): String = C(42).test()
