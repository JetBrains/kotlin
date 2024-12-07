// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

class Foo(val s: String): Comparable<Foo> {
    override fun compareTo(other: Foo): Int {
        return s.compareTo(other.s)
    }
}

fun box(): String {
    try {
        val x = (Foo("1") as Comparable<Any>).compareTo(2)
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
