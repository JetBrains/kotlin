class My(var x: Int) {
    operator fun invoke() = x

    fun foo() {}

    fun copy() = My(x)
}

fun testInvoke(): Int = My(13)()