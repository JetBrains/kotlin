// LANGUAGE: +ContextParameters

context(c: Int)
context(c: String)
fun syncWithSheets() {}

context(c: Int)
context(c: String)
val f: Int get() = c

class Foo {
    context(c: Int)
    context(c: String)
    constructor()
}
