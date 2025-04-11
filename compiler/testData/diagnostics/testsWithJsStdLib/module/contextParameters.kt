// LANGUAGE: +ContextParameters
@file:JsModule("lib")

external class Scope1
external class Scope2

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>, scope2: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope2<!>)<!>
external fun foo()<!>

<!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope2: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope2<!>, scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>)<!>
external fun foo()<!>
