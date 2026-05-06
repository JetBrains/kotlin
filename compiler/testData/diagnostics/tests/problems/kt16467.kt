// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-16467
// WITH_STDLIB

// KT-16467: No compiler error for inline function lambda parameter called from anonymous object on implicit receiver
inline fun test(f: () -> Unit) {
    <!USAGE_IS_NOT_INLINABLE!>f<!>.apply {
        object {
            init {
                invoke() // no error in K1, compiler crash; should be NON_LOCAL_RETURN_NOT_ALLOWED
            }
        }
    }
}

fun main() {
    test {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, functionalType, init, inline, lambdaLiteral */
