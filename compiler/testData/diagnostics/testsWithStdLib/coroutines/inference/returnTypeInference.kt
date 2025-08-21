// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class Controller

fun <R> generate(g: suspend Controller.() -> R): R = TODO()

val test1 = generate {
    3
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, propertyDeclaration, suspend, typeParameter, typeWithExtension */
