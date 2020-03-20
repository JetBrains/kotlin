private val x = object {
    fun foo(x: Int) = /* here we have not computed return type for "bar" */bar(x)
    fun bar(y: Int) = this.hashCode() + y > 0

    val w get() = z
    val z get() = this.hashCode() == 0
}

fun useBoolean(b: Boolean) {}

fun main() {
    useBoolean(x.foo(1))
    useBoolean(x.bar(2))
    useBoolean(x.w)
    useBoolean(x.z)
}
