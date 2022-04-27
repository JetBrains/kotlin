class D {
    fun stable(c: C): String = "K"
    fun foo(): String = stable(C())

    fun exp(e: E): String = "FAIL1"
    fun bar(): String = exp(E())
}
