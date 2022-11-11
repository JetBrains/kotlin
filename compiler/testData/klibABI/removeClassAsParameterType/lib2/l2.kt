class D {
    fun stable(c: C): String = "OK"
    fun foo(): String = stable(C())

    fun exp(e: E?): String = "FAIL1"
    fun bar(): String = exp(null)

    var exp: E? = null
        protected set(value) { /* Do nothing */ }

    fun baz(): String {
        exp = null
        return "FAIL2"
    }
}
