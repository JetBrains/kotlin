@Target(AnnotationTarget.TYPE)
annotation class Anno

interface Recursive<R : Recursive<@Anno R>>

class Example<R : Recursive<@Anno R>> {
    fun nested() : NestedExample<R> = TODO()
}

class NestedExample<R : Recursive<@Anno R>> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*> = TODO()

fun main() {
    <expr>example.nested()</expr>.function(1)
}
