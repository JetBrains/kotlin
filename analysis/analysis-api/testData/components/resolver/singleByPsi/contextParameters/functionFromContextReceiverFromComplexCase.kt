class Receiver

class Container {
    context(Receiver)
    fun c() {}
}

context(Receiver, Container)
class Foo {
    init {
        <expr>c()</expr>
    }
}

// LANGUAGE: +ContextReceivers