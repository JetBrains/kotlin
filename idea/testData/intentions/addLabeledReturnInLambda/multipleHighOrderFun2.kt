// WITH_RUNTIME
// INTENTION_TEXT: "Add return@bar"

private fun <T> List<T>.bar(a: (T) -> Boolean, b: (T) -> Boolean) {}

fun foo() {
    listOf(1,2,3).bar({
        <caret>true
    }) {
        true
    }
}