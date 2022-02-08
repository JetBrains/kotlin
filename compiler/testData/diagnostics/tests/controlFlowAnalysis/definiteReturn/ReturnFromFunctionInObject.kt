// FIR_IDENTICAL
interface X {
    fun f(): Boolean
}

val m = object : X {
    override fun f(): Boolean {
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

    fun foo() {
        fun local(): Int {
        <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
    }
}
