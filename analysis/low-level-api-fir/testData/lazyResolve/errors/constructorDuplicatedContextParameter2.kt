// LANGUAGE: +ContextParameters

class Foo {
    context(c: Int)
    context(c<caret>: String)
    constructor()
}
