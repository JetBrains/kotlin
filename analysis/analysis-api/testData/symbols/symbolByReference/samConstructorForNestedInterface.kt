package test

class Outer {
    fun interface Nested {
        fun f()
    }
}

fun usage() {
    Outer.<caret>Nested { }
}
