// LANGUAGE: +ContextParameters
// FILE: DeclarationOverloads.kt
package DeclarationOverloads

<!CONFLICTING_OVERLOADS!>fun test()<!> {}

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!> fun test()<!> = <!UNRESOLVED_REFERENCE!>x<!>

val <!REDECLARATION!>test<!> = 0

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!> val <!REDECLARATION!>test<!> get() = <!UNRESOLVED_REFERENCE!>x<!>
