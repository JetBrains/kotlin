interface I

class A : I {
    fun f1(other: Any?): Int =
        if (other is I) other.hashCode() else 0

    inline fun <reified T : I> f2(other: Any): Int =
        if (other is T) other.hashCode() else 0

    fun f3() {
        f2<A>(A())
    }
}

// 3 INVOKEVIRTUAL java/lang/Object.hashCode \(\)I
// 0 INVOKEINTERFACE
