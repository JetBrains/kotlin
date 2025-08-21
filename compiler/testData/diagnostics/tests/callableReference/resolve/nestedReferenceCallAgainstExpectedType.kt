// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B

fun foo(a: A) {}
fun foo(b: B) {}

fun bar(a: A) {}

val l0: (A) -> Unit
    get() =
        if (1 < 2) {
            ::foo
        } else {
            ::bar
        }

val l1: (A) -> Unit
    get() = when {
        true -> ::foo
        false -> { ::foo }
        else -> ::bar
    }

/* GENERATED_FIR_TAGS: callableReference, comparisonExpression, functionDeclaration, functionalType, getter,
ifExpression, integerLiteral, interfaceDeclaration, propertyDeclaration, whenExpression */
