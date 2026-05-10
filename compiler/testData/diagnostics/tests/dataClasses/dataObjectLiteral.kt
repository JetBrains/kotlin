// RUN_PIPELINE_TILL: FRONTEND

interface I {
    fun foo()
}

val o = <!UNRESOLVED_REFERENCE!>data<!><!SYNTAX!><!> object<!SYNTAX!><!>: I {
    override fun foo() {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, objectDeclaration, override, propertyDeclaration */
