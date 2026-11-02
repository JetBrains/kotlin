// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

var withAccessor: context(Int)
    <!UNRESOLVED_REFERENCE!>get<!><!SYNTAX!>(<!><!SYNTAX!>) = TODO()<!>

val delegated: context(Int) <!UNRESOLVED_REFERENCE!>by<!> <!SYNTAX!>lazy<!><!SYNTAX!><!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ TODO() }<!>

fun body() {
    val local: context(Int)
    <!UNRESOLVED_REFERENCE!>println<!><!SYNTAX!>(<!><!SYNTAX!>)<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, localProperty, propertyDeclaration, typeWithContext */
