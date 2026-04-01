// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

interface I {
    context(s: String)
    fun simple()

    context(a: String)
    fun swapNames(b: Int)

    context(s: String)
    fun unnamedInOverride()

    context(_: String)
    fun unnamedInBase()
}

class C : I {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>string<!>: String)
    override fun simple() {}

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>b<!>: String)
    override fun swapNames(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>a<!>: Int) {}

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>_<!>: String)
    override fun unnamedInOverride() {}

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>s<!>: String)
    override fun unnamedInBase() {}
}

fun test(c: C) {
    c.<!NO_CONTEXT_ARGUMENT!>simple<!>(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
    c.simple(string = "")
    (c as I).simple(s = "")
    (c as I).<!NO_CONTEXT_ARGUMENT!>simple<!>(<!NAMED_PARAMETER_NOT_FOUND!>string<!> = "")

    c.swapNames(42, b = "")
    c.<!NO_CONTEXT_ARGUMENT!>swapNames<!>(42, <!ARGUMENT_PASSED_TWICE!>a<!> = "")
    c.swapNames(b = "", a = 42)
    c.swapNames(a = <!ARGUMENT_TYPE_MISMATCH!>""<!>, b = <!ARGUMENT_TYPE_MISMATCH!>42<!>)
    (c as I).swapNames(42, a = "")
    (c as I).<!NO_CONTEXT_ARGUMENT!>swapNames<!>(42, <!ARGUMENT_PASSED_TWICE!>b<!> = "")
    (c as I).swapNames(a = "", b = 42)
    (c as I).swapNames(b = <!ARGUMENT_TYPE_MISMATCH!>""<!>, a = <!ARGUMENT_TYPE_MISMATCH!>42<!>)

    c.<!NO_CONTEXT_ARGUMENT!>unnamedInOverride<!>(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
    (c as I).unnamedInOverride(s = "")

    c.unnamedInBase(s = "")
    (c as I).<!NO_CONTEXT_ARGUMENT!>unnamedInBase<!>(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
stringLiteral */
