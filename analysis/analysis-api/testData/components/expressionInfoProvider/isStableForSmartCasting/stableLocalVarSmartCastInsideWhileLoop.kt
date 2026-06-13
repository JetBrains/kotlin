interface Node {
    val parent: Node?
}

fun test(initial: Node) {
    var current: Node? = initial

    while (current != null) {
        consume(<expr>current</expr>)
        current = current.parent
    }
}

fun consume(node: Node) {}
