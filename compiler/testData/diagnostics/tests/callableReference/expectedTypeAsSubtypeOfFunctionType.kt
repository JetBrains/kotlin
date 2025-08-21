// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

abstract class SubFunction : kotlin.Function0<Unit>

fun <T> takeIt(x: T, f: SubFunction) {}

fun cr() {}

fun test() {
    takeIt(42, <!TYPE_MISMATCH!>::cr<!>)
    takeIt(42, <!TYPE_MISMATCH!>{ }<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral, nullableType, typeParameter */
