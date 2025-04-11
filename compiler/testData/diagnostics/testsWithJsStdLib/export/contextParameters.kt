// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ContextParameters
@JsExport
class Scope1 {
    fun foo() {}
}

class Scope2 {
    fun bar() {}
}

@JsExport
<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>, scope2: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope2<!>)<!>
fun test1() {
    <!UNRESOLVED_REFERENCE!>scope1<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()
    <!UNRESOLVED_REFERENCE!>scope2<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>()
}

@JsExport
fun <A, B, R> context1(a: A, b: B, block: <!SUBTYPING_BETWEEN_CONTEXT_RECEIVERS, UNSUPPORTED_FEATURE!>context(A, B)<!> () -> R): R = block(a, b)

@JsExport
fun test2(scope1: Scope1, <!NON_EXPORTABLE_TYPE!>scope2: Scope2<!>){
    context1(scope1, scope2) {
        test1()
    }
}

@JsExport
class C {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(scope1: <!DEBUG_INFO_MISSING_UNRESOLVED!>Scope1<!>)<!>
    @JsExport.Ignore
    fun test() {}
}
