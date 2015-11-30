// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

infix fun Int.add(other: Int) = this + other

fun foo() {
    1<caret>!! add 2
}