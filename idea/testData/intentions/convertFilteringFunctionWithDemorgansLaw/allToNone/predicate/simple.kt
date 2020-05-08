// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = list.<caret>all { it.isFoo }
}

private val Int.isFoo
    get() = true