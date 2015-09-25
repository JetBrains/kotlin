fun registerHandler(handler: Handler<out Message>) {}

fun test() {
    registerHandler(<caret>)
}

interface Message

interface Handler<E> {
    fun handle(e: E)
}

// ELEMENT: object