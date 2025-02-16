class Receiver

class Container {
    context(Receiver)
    fun c() {}
}

context(Receiver, Container)
fun Boolean.bar() {
    <expr>c()</expr>
}

// LANGUAGE: +ContextReceivers