fun test() {
    val test = when {<caret>1

    fun foo() = 42
}
//-----
fun test() {
    val test = when {
        <caret>1

            fun foo() = 42
    }
}