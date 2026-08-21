// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect fun foo(
    x: String = "O",
    @IntroducedAt("2") y: String = "foo",
): String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun foo(
    x: String,
    @IntroducedAt("2") y: String,
): String = x + y

/* GENERATED_FIR_TAGS: actual, additiveExpression, expect, functionDeclaration, stringLiteral */
