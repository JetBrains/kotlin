package test

class Outer {
    fun interface Nested {
        fun f()
    }
}

fun interface Nested {
    fun g(x: Int)
}

fun usage() {
    Outer.<caret>Nested { }
}
