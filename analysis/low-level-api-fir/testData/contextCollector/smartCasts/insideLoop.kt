interface Node {
    val parent: Node?
}

fun test(initial: Node) { 
    var current = initial
    
    while (current != null) {
        <expr>consume(current)</expr>
        current = current.parent
    }
}

fun consume(node: Node) {}