// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(c: String)
fun foo(s: String, s2: String) {}

fun test() {
    foo(s = "", "", c = "")
    foo(c = "2", <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>""<!>, <!NO_VALUE_FOR_PARAMETER!>s = "")<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, stringLiteral */
