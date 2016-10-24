// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    listOf(1).forEach { run { <caret>it.bar() } }
}

fun Int.bar() {
}