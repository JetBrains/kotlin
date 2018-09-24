// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
data class Node(private val data: MutableList<String>) {
    fun add(word: String) {
        data.add(word)
    }
}

fun foo(node: Node, words: List<String>) {
    <caret>for (word in words) {
        node.add(word)
    }
}
