// WITH_RUNTIME

fun test(list: List<Int>) {
    val elementAtOrElse: Int = list.<caret>filter { it > 1 }.elementAtOrElse(1) { 1 }
}