interface Node {
    val parent: Node?
}

fun test(initial: Node?) {
    var current = initial

    while (initial!! != null) {
        consume(current)
        current = current.parent
    }

    <expr>consume(initial)</expr>
}

fun consume(node: Node) {}