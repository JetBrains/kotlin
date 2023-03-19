// WITH_STDLIB
// IGNORE_FE10
fun foo(list: List<Int>) {
    val result: List<String> = list.map { a<caret>v }
}