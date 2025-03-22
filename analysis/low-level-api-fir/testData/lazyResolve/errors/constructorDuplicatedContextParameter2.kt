// LANGUAGE: +ContextParameters
// IGNORE_FIR

class Foo {
    context(c: Int)
    context(c<caret>: String)
    constructor()
}