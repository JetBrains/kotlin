// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
class Foo

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(Foo, label@Int)
fun foo() {
    <!NO_COMPANION_OBJECT!>Foo<!>
    <!UNRESOLVED_REFERENCE!>label<!>
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(Foo, label@Int)
val bar: String
    get() {
        <!NO_COMPANION_OBJECT!>Foo<!>
        <!UNRESOLVED_REFERENCE!>label<!>
        return ""
    }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, getter,
propertyDeclaration, propertyDeclarationWithContext, stringLiteral */
