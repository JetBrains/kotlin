// WITH_STDLIB
fun foo() {
    val lst = mutableListOf<List<*>>()
    lst[0] = emptyList<Any>()
    <expr>lst[0]</expr>
}
