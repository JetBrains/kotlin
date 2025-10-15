// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class C {
    context(s: String)
    operator fun get(i: Int) = ""

    context(s: String)
    operator fun invoke(i: Int) = ""
}

fun test(c: C, f: context(String) () -> Unit) {
    <!NO_CONTEXT_ARGUMENT!>c[1, <!ASSIGNMENT_IN_EXPRESSION_CONTEXT, TOO_MANY_ARGUMENTS!><!UNRESOLVED_REFERENCE!>s<!> = ""<!>]<!>

    c(1, s = "")
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
operator, stringLiteral */
