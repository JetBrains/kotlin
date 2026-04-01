// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +MultiPlatformProjects +DataFlowBasedExhaustiveness

// MODULE: common
// During metadata compilation we get `expect open class Any`
expect open class ExpectAny()

sealed class Variants : ExpectAny() {
    data object A : Variants()
    data object B : Variants()
}

fun foo(v: Variants): String {
    // Must not be `UNSAFE_EXHAUSTIVENESS` during metadata compilation
    return when (v) {
        Variants.A -> "A"
        Variants.B -> "B"
    }
}

// MODULE: jvm()()(common)

actual open class ExpectAny

fun bar() = Variants.A

/* GENERATED_FIR_TAGS: actual, classDeclaration, data, equalityExpression, expect, functionDeclaration, nestedClass,
objectDeclaration, primaryConstructor, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
