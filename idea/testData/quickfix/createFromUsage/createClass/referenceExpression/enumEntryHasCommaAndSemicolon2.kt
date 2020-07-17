// "Create enum constant 'C'" "true"
enum class Baz {
    A,
    B, ;
}

fun main() {
    Baz.C<caret>
}