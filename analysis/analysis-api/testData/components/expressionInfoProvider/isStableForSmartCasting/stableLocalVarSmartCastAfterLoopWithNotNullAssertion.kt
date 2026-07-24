interface Node {
    val shouldProcess: Boolean
    val parent: Node?
}

fun test(initial: Node?) {
    var current = initial

    while (current!!.shouldProcess) {
        current = current.parent
    }

    consume(<expr>current</expr>)
}

fun consume(node: Node) {}
