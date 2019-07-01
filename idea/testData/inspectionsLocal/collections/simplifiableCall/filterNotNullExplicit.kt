// WITH_RUNTIME
fun test(list: List<String?>) {
    list.<caret>filter { arg -> arg != null }
}