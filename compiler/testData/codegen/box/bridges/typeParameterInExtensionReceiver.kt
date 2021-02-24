interface A<T1> {
    fun <T2> T2.foo(x: T1): String
}

class Z : A<String> {
    override fun <T2> T2.foo(x: String) = this.toString() + x
}

fun A<String>.result() = "O".foo("K")

fun box(): String {
    val a: A<String> = Z()
    return a.result()
}
