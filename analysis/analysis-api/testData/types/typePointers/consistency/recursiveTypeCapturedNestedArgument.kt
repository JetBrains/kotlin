interface Node<T>

class Example<R : Node<List<R?>>> {
    fun nested() : NestedExample<R> = TODO()
}

class NestedExample<R : Node<List<R?>>> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*> = TODO()

fun main() {
    <expr>example.nested()</expr>.function(1)
}
