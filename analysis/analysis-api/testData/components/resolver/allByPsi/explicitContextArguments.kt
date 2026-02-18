class Implicit
class Explicit

context(_: Implicit, explicit: Explicit)
fun unnamed(regular: String) {}

context(s: String, explicit: Explicit)
fun unnamed(regular: String) {}

context(implicit: Implicit, explicit: Explicit)
fun named(regular: String) {}

context(s: String, explicit: Explicit)
fun named(regular: String) {}

context(_: Implicit, explicit: Explicit)
fun contextualUsage() {
    unnamed/*explicit regular*/(regular = "str2")
    named/*explicit regular*/(regular = "str2")

    unnamed/*all explicit*/(explicit = Explicit(), regular = "str3")
    named/*all explicit*/(explicit = Explicit(), regular = "str3")
}

fun Implicit.receiverUsage() {
    with(Explicit()) {
        unnamed/*explicit regular*/(regular = "str2")
        named/*explicit regular*/(regular = "str2")
    }

    unnamed/*all explicit*/(explicit = Explicit(), regular = "str3")
    named/*all explicit*/(explicit = Explicit(), regular = "str3")
}

// LANGUAGE: +ContextParameters +ExplicitContextArguments