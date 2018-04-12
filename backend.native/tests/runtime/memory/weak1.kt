package runtime.memory.weak1

import kotlin.test.*
import konan.ref.*

class Node(var next: Node?)

@Test fun runTest() {
    val node1 = Node(null)
    val node2 = Node(node1)
    node1.next = node2

    konan.ref.WeakReference(node1)
    println("OK")
}
