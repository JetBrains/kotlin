package q

@native
class Foo {
    // RUN:
    fun foo(s: Array<String>) = noImpl
}


// RUN:
@native
fun main(s: Array<String>) {
    println("Top-level")
}