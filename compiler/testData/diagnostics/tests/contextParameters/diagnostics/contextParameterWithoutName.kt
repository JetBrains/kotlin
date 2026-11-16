// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87288
// LANGUAGE: +ContextParameters

context(_: String)
fun useString() {}

context(_: Int)
fun useInt() {}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>, <!CONTEXT_PARAMETER_WITHOUT_NAME!>Int<!>)
fun legacyContextReceiverSyntax() {
    useString()
    useInt()
}

context(<!SYNTAX!><!>: String)
fun missingName() {
    useString()
}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>(s: String) -> Unit<!>)
fun colonInType() {}

private class Private

context(<!CONTEXT_PARAMETER_WITHOUT_NAME, EXPOSED_PARAMETER_TYPE!>Private<!>)
fun exposedType() {}

interface Base {
    context(s: String)
    fun foo()
}

class Derived : Base {
    context(<!CONTEXT_PARAMETER_WITHOUT_NAME, PARAMETER_NAME_CHANGED_ON_OVERRIDE!>String<!>)
    override fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, functionalType,
interfaceDeclaration, override */
