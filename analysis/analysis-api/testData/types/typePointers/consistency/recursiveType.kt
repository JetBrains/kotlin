interface Recursive<R : Recursive<R>>

class Example<R : Recursive<R>> {
    fun nested() : NestedExample<R> = TODO()
}

class NestedExample<R : Recursive<R>> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*> = TODO()

fun main() {
    val nested = example.nested()
    <expr>nested</expr>.function(1)
}