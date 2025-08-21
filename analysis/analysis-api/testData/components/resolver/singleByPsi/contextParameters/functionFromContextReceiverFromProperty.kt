class Receiver

class Container {
    fun c() {}
}

context(Container)
val Receiver.f: Unit
    get() {
    <expr>c()</expr>
}

// LANGUAGE: +ContextReceivers