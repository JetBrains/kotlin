


class D {
    fun foo(): String = stable(C())
    fun stable(c: C): String = "K"

    fun bar(): String = exp(E())
    fun exp(e: E): String = "FAIL1"
}