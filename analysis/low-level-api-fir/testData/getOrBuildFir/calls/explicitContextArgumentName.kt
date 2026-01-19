class Implicit
class Explicit

context(_: Implicit, explicit: Explicit)
fun unnamed(regular: String) {}

fun Implicit.receiverUsage() {
    unnamed(<expr>explicit</expr> = Explicit(), regular = "str")
}

// LANGUAGE: +ContextParameters +ExplicitContextArguments