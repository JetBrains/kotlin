// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int?>) {
    listOf("string").map {
        list.<caret>map other@{
            return@map it!!
        }
        "s"
    }
}