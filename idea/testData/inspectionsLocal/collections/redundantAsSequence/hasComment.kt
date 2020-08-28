// WITH_RUNTIME
fun test(list: List<String>) {
    list/*comment*/.<caret>asSequence().last()
}