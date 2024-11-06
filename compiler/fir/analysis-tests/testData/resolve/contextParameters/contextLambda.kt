// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, +ContextParameters

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(_: String)
fun foo() {}

fun acceptLambda(f: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) () -> Unit) {}

fun test() {
    acceptLambda {
        foo()
        <!UNRESOLVED_REFERENCE!>length<!>
    }
}
