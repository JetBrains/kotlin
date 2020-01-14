// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>asSequence().all { it.isBlank() }
}