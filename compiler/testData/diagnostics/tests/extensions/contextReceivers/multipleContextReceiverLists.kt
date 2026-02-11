// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters

<!MULTIPLE_CONTEXT_LISTS!>context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
fun foo() {}

<!MULTIPLE_CONTEXT_LISTS!>context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
val bar: String get() = ""

<!MULTIPLE_CONTEXT_LISTS!>context(String) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
class C

class D {
    <!MULTIPLE_CONTEXT_LISTS!>context(<!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>) context(<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
    constructor()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, getter,
propertyDeclaration, propertyDeclarationWithContext, secondaryConstructor, stringLiteral */
