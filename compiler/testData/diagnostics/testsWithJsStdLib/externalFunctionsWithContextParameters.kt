// LANGUAGE: +ContextParameters
external class Scope1
class Scope2

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>, scope2: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope2<!>)<!>
external fun foo()

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>, scope2: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope2<!>)<!>
fun bar() {
    foo()
}

external fun <A, B, R> context(a: A, b: B, block: <!SUBTYPING_BETWEEN_CONTEXT_RECEIVERS, UNSUPPORTED_FEATURE!>context(A, B)<!> () -> R): R

fun baz(scope1: Scope1, scope2: Scope2) {
    context(scope1, scope2) {
        foo()
    }
}
