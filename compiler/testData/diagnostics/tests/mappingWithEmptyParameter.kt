// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81625
// LANGUAGE: +ContextParameters
// FIR_IDENTICAL
//  ^ K1 ignored
// RENDER_DIAGNOSTIC_ARGUMENTS

fun normalParams(first: Int, <!SYNTAX(": Expecting a parameter declaration")!><!>, third: Char) {}

context(<!SYNTAX(": Type expected")!><!>, str: String)
fun contextParams() {}

val functionalType: (<!SYNTAX(": Type expected")!><!>,) -> Unit = <!SYNTAX(": Incomplete code")!>{ }<!>
val contextualFunctionalType: context(<!SYNTAX(": Type expected")!><!>, String) () -> Unit = <!SYNTAX(": Incomplete code")!>{ }<!>


fun useSite() {
    normalParams(42, "missed", 'a')
    normalParams(42, <!SYNTAX(": Expecting an argument")!><!>, 'a')
    normalParams<!NO_VALUE_FOR_PARAMETER("<no name provided>")!>(first = 42, <!NAMED_PARAMETER_NOT_FOUND("<no name provided>")!>`<no name provided>`<!> = "str", third = 'c')<!>
    functionalType("missed")
    functionalType<!NO_VALUE_FOR_PARAMETER("p1")!>()<!>
    functionalType(<!SYNTAX(": Expecting an argument")!><!>,)

    with("context") {
        contextParams()
        contextualFunctionalType()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, integerLiteral,
lambdaLiteral, propertyDeclaration, stringLiteral */
