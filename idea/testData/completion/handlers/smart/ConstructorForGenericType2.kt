fun registerHandler(handler: Handler<out Message>) {}

fun test() {
    registerHandler(<caret>)
}

trait Message

trait Handler<E> {
    fun handle(e: E)
}

// ELEMENT: object