// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(
    @IntroducedAt("1") a: Int = <!UNINITIALIZED_PARAMETER!>b<!>,
@IntroducedAt("2") b: Int = 2,
) {}

private fun alsoFun(x: Int) = x
fun also(
    @IntroducedAt("1") a: Int = 0.also { alsoFun(<!UNINITIALIZED_PARAMETER!>b<!>) },
    @IntroducedAt("2") b: Int = 2,
) {
}

fun localFun(
    @IntroducedAt("1") a: Int = run {
        fun c(x: Int) = x + 1
        c(<!UNINITIALIZED_PARAMETER!>b<!>)
    },
    @IntroducedAt("2") b: Int = 2,
) {
}

fun lambda(
    @IntroducedAt("1") a: () -> Int = { <!UNINITIALIZED_PARAMETER!>b<!> },
    @IntroducedAt("2") b: Int = 2,
) {
}

fun coll(
    @IntroducedAt("1") a: Int = listOf(1).map { it + <!UNINITIALIZED_PARAMETER!>b<!> }.first(),
    @IntroducedAt("2") b: Int = 2,
) {
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationUseSiteTargetFile, classReference, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, localFunction, stringLiteral */
