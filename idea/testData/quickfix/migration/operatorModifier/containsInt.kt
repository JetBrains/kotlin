// "Add 'operator' modifier" "false"
// ACTION: Convert member to extension
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
class A {
    fun <caret>contains(other: A): Int = -1
}
