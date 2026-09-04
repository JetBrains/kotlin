// WITH_STDLIB

class Node(val value: Int, val parent: Node? = null)

fun box(): String {
    val tree = Node(1, Node(2, Node(3)))
    val result = generateSequence(tree) { it.parent }.last().value
    if (result != 3) return "Fail: $result"
    return "OK"
}
