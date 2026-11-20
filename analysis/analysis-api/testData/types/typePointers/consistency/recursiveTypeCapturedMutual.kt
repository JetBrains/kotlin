interface Pair<A, B>

class Example<L : Pair<R?, L>, R : Pair<L?, R>> {
    fun nested() : NestedExample<L, R> = TODO()
}

class NestedExample<L : Pair<R?, L>, R : Pair<L?, R>> {
    fun function(param: String) : Unit = TODO()
}

val example: Example<*, *> = TODO()

fun main() {
    <expr>example.nested()</expr>.function(1)
}
