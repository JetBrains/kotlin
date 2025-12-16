// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82863
// DIAGNOSTICS: -ERROR_SUPPRESSION

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> fooTakingLambda(fn: () -> T): @kotlin.internal.NoInfer T = fn()

fun bar() {
    val x: Unit = fooTakingLambda { 42 }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
