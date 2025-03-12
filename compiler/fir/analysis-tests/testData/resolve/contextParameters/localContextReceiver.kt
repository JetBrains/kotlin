// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextReceivers
// ISSUE: KT-75050

class A(val a: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
private fun f1() {
    <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
    fun f2() {
        foo(a)
    }

    f2()
}

fun foo(s: String) {

}
