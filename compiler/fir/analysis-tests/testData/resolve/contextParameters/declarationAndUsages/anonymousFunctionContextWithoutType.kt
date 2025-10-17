// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun foo(f: context(String) () -> Unit) {}

fun test(c: String) {
    // Invalid syntax: context call and anonymous function declaration are separated
    foo(<!NONE_APPLICABLE!>context<!>(c)<!SYNTAX!><!> <!TOO_MANY_ARGUMENTS!>fun() { }<!>)
}

fun test2() {
    // Correct: pass an anonymous function with the context parameters
    foo(context(x: String) fun() { })
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, typeWithContext */
