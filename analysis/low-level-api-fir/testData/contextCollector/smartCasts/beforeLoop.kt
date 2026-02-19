interface Node {
    val parent: Node?
}

fun test(initial: Node) { 
    <expr>var current = initial</expr>
    
    while (current != null) {
        consume(current)
        current = current.parent
    }
}

fun consume(node: Node) {}