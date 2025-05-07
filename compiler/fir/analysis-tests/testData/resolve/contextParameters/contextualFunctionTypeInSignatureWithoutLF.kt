// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
fun foo1(f: <!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit) {}
fun (<!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit).foo2() {}
fun foo3(): <!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <T : <!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit> foo4() {}

fun bar() {
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>foo1 {}<!>
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>({}).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo2<!>()<!>
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>foo3()<!>
    <!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>foo4<<!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit>()<!>
}
