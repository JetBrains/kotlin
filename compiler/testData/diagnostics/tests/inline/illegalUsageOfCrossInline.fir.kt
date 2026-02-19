// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79545
abstract class A(val x: () -> Unit)

inline fun f(crossinline x: () -> Unit) {
    object : A(<!USAGE_IS_NOT_INLINABLE!>x<!>) {}
}

fun main() {
    f {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, crossinline, functionDeclaration, functionalType,
inline, lambdaLiteral, primaryConstructor, propertyDeclaration */
