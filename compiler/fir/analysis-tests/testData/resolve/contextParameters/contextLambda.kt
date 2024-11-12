// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(_: String)
fun foo() {}

fun acceptLambda(f: context(String) () -> Unit) {}

fun test() {
    acceptLambda {
        foo()
        <!UNRESOLVED_REFERENCE!>length<!>
    }
}
