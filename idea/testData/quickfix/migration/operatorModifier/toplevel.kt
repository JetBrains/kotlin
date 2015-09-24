// "Add 'operator' modifier" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
class A {
}

fun <caret>plus(other: A): A = A()
