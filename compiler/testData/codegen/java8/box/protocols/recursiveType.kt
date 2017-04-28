// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface PNode {
    fun info(): String

    fun left(): PNode?
    fun right(): PNode?
}

class Node {
    fun info(): String = "OK"

    fun left(): PNode? = null
    fun right(): PNode? = null
}

fun box(): String {
    val x: PNode = Node()

    if (x.left() != x.right()) {
        return "FAIL"
    }

    return x.info()
}