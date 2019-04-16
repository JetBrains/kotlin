// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapIndexed label@{ index, value ->
        if (index == 0) return@label 0
        index + 42
    }
}