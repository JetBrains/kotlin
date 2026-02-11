// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// ISSUE: KT-75050

class A(val a: String)

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(A)
private fun f1() {
    <!NONE_APPLICABLE, NO_VALUE_FOR_PARAMETER!>context<!>(<!NO_COMPANION_OBJECT!>A<!>)
    fun f2() {
        foo(<!UNRESOLVED_REFERENCE!>a<!>)
    }

    f2()
}

fun foo(s: String) {

}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, localFunction,
primaryConstructor, propertyDeclaration */
