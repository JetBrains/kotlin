// WITH_STDLIB
fun foo() {
    val lst = mutableListOf<List<*>>()
    <expr>lst</expr>[0] = emptyList<Any>()
}
