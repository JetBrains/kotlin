// IS_APPLICABLE: true
// WITH_RUNTIME

fun foo() {
    listOf(1).forEach { (-it).let(<caret>it::bar) }
}

fun Int.bar(arg: Int) {
}