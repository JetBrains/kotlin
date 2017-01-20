interface A {
    fun foo(): String
}

class B : A {
    override fun foo() = "OK"
}

fun box() = (A::foo)(B())
