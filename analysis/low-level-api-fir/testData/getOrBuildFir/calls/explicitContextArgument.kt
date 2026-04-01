class Implicit
class Explicit

context(_: Implicit, explicit: Explicit)
fun unnamed(regular: String) {}

fun Implicit.receiverUsage() {
    unnamed(explicit = <expr>Explicit()</expr>, regular = "str")
}

// LANGUAGE: +ContextParameters +ExplicitContextArguments