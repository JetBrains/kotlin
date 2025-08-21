class Receiver

class Container {
    fun c() {}
}

context(Container)
class Foo {
    init {
        <expr>c()</expr>
    }
}

// LANGUAGE: +ContextReceivers