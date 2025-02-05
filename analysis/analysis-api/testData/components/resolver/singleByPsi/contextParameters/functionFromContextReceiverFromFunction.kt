class Receiver

class Container {
    fun c() {}
}

context(Container)
fun Receiver.f() {
    <expr>c()</expr>
}

// LANGUAGE: +ContextReceivers