package q

external class Foo {
    // RUN:
    fun foo(s: Array<String>) = noImpl
}


// RUN:
external fun main(s: Array<String>) {
    println("Top-level")
}