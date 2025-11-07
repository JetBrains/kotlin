// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -NON_ASCENDING_VERSION_ANNOTATION
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun foo(
        a: Int,
        @IntroducedAt("1") a1: Int = 1,
        @IntroducedAt("2") b: Int = a + a1,
    ) { }

    fun foo2(
        a: Int = 1,
        @IntroducedAt("2") a1: Int = 1,
        @IntroducedAt("2") a2: Int = a + 1,
        @IntroducedAt("1") b: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a1<!> + 1,
        @IntroducedAt("1") c: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a1<!> * <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a2<!>,
    ) { }
}

data class D(
    val a: Int = 1,
    @IntroducedAt("2") val a1: Int = 1,
    @IntroducedAt("2") val a2: Int = a + 1,
    @IntroducedAt("1") val b: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a1<!> + 1,
    @IntroducedAt("1") val c: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a1<!> * <!INVALID_DEFAULT_VALUE_DEPENDENCY("1; 2")!>a2<!>,
)

fun foo(
    x: String,
    y: (String) -> String,
    @IntroducedAt("2") z: String = x,
    @IntroducedAt("2") s: String = "hello",
    @IntroducedAt("1") u: String = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>s<!>,
    @IntroducedAt("1") v: String = y(""),
) { }

fun bar(@IntroducedAt("1") x: Int = 10, y: Int = <!INVALID_DEFAULT_VALUE_DEPENDENCY!>x<!>) {}

/* GENERATED_FIR_TAGS: additiveExpression, annotationUseSiteTargetFile, classDeclaration, classReference, data,
functionDeclaration, functionalType, integerLiteral, lambdaLiteral, multiplicativeExpression, primaryConstructor,
propertyDeclaration, stringLiteral */
