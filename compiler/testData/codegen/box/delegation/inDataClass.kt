interface A {
    fun foo(): String
    val bar: String
}

class B : A {
    override fun foo(): String = "O"
    override val bar: String get() = "K"
}

data class C(val a: A): A by a

fun box() = C(B()).let { it.foo() + it.bar }
