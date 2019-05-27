// WITH_RUNTIME
fun test() {
    val test = run {<caret>1

    fun foo() = 42
}
//-----
// WITH_RUNTIME
fun test() {
    val test = run {
        <caret>1

        fun foo() = 42
    }
}