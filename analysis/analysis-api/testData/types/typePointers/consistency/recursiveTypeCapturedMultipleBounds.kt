interface Recursive<R : Recursive<R>>

class Example<R> where R : Recursive<R>, R : Comparable<R?> {
    fun nested() : NestedExample<R> = TODO()
}

class NestedExample<R> where R : Recursive<R>, R : Comparable<R?> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*> = TODO()

fun main() {
    <expr>example.nested()</expr>.function(1)
}
