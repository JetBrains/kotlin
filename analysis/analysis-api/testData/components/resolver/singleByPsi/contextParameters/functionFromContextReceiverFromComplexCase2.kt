class Receiver

class Container {
    context(Receiver)
    fun c() {}
}

context(Container, Receiver)
class Foo {
    init {
        <expr>c()</expr>
    }
}

// LANGUAGE: +ContextReceivers