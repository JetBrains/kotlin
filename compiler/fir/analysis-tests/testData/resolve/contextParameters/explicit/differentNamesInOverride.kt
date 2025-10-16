// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

interface I {
    context(s: String)
    fun simple1()

    context(s: String)
    fun unnamedInOverride()

    context(_: String)
    fun unnamedInBase()
}

class C : I {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>string<!>: String)
    override fun simple1() {}

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>_<!>: String)
    override fun unnamedInOverride() {}

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>s<!>: String)
    override fun unnamedInBase() {}
}

fun test(c: C) {
    c.<!NO_CONTEXT_ARGUMENT!>simple1<!>(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
    c.simple1(string = "")

    c.<!NO_CONTEXT_ARGUMENT!>unnamedInOverride<!>(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")

    c.unnamedInBase(s = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
stringLiteral */
