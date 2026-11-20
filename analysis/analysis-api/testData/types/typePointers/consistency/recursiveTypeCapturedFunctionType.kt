class Example<R : Comparable<(R?) -> R>> {
    fun nested() : NestedExample<R> = TODO()
}

class NestedExample<R : Comparable<(R?) -> R>> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*> = TODO()

fun main() {
    <expr>example.nested()</expr>.function(1)
}
