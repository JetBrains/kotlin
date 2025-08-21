class Receiver

class Container {
    context(Receiver)
    fun c() {}
}

context(Receiver, Container)
fun bar() {
    <expr>c()</expr>
}

// LANGUAGE: +ContextReceivers