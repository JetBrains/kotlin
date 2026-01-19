// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression

class Implicit
class Explicit

context(_: Implicit, explicit: Explicit)
fun unnamed(regular: String) {}

fun Implicit.receiverUsage() {
    unnamed(<expr>explicit</expr> = Explicit(), regular = "str")
}

// LANGUAGE: +ContextParameters +ExplicitContextArguments