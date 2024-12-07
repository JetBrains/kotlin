interface Node {
    val shouldProcess: Boolean
    val parent: Node?
}

fun test(initial: Node?) {
    var current = initial

    while (current!!.shouldProcess) {
        consume(current)
        current = current.parent
    }

    <expr>consume(current)</expr>
}

fun consume(node: Node) {}