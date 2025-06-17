// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77986

inline fun test(crossinline onInit: () -> Unit) {
    run {
        class Local(val x: Int) {
            init { onInit() }
        }
        Local(1)
    }
}

fun main() {
    test {}
}

/* GENERATED_FIR_TAGS: classDeclaration, crossinline, functionDeclaration, functionalType, init, inline, integerLiteral,
lambdaLiteral, localClass, primaryConstructor, propertyDeclaration */
