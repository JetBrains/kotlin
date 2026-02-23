interface Base {
    fun accept(i: Int): Boolean
}

fun interface Derived : Base

fun usage(p: <caret>Derived) {}
