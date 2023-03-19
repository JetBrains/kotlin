// WITH_STDLIB
fun foo(list: List<Int>) {
    val result: List<Int> = list.map { mapOf(a<caret>v) }
}