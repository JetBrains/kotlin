// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

<!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(c: A)
val prop1: String = ""

context(c: A)
var prop2: String = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>""<!>
    get() = ""
    set(value) {
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, getter, propertyDeclaration,
propertyDeclarationWithContext, setter, stringLiteral */
