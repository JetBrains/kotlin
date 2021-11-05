// WITH_STDLIB
fun foo() {
    val lst = listOf<List<*>>(listOf("A"), listOf(42), emptyList())
    <expr>lst[0]</expr>
}
