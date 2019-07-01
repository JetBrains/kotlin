// WITH_RUNTIME
fun test(list: List<String?>) {
    list.<caret>filter { null != it }
}