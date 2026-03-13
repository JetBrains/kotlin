// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(c: String)
fun foo(s: String, s2: String) {}

context(c: String)
fun foo1(s: String, vararg s2: String) {}

fun test() {
    foo(s = "", "", c = "")
    foo<!NO_VALUE_FOR_PARAMETER!>(c = "2", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>, s = "")<!>

    foo1(s = "", "", "", c = "")
    foo1(c = "", s = "", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>)
    foo1(s = "", c = "", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>)
    foo1(s = "", "", c = "", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, stringLiteral */
