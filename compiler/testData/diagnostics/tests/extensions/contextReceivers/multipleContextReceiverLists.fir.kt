// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) context(Int)
fun foo() {}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) context(Int)
val bar: String get() = ""

<!CONTEXT_CLASS_OR_CONSTRUCTOR, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) context(Int)
class C

class D {
    context(String) context(Int)
    constructor()
}
