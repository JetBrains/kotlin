// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers

<!MULTIPLE_CONTEXT_LISTS!><!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String)<!> context(Int)
fun foo() {}

<!MULTIPLE_CONTEXT_LISTS!><!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String)<!> context(Int)
val bar: String get() = ""

<!MULTIPLE_CONTEXT_LISTS!><!CONTEXT_CLASS_OR_CONSTRUCTOR, CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String)<!> context(Int)
class C

class D {
    <!MULTIPLE_CONTEXT_LISTS!>context(String)<!> context(Int)
    constructor()
}
