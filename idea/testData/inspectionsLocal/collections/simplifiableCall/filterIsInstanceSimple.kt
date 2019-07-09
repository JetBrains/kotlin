// WITH_RUNTIME
fun test(list: List<Any>) {
    list.<caret>filter { it is String }
}