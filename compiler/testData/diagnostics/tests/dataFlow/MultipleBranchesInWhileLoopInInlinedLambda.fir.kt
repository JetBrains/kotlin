// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_VARIABLE, +UNUSED_VALUE
// WITH_EXTRA_CHECKERS

inline fun execute(func: () -> Unit) { func() }
fun conditionA(): Boolean { return false }
fun conditionB(): Boolean { return true }

fun main() {
    execute {
        var value = 0
        while (true) {
            if (conditionA()) return
            if (conditionB()) {
                value.run { if (this > 0) return@execute }
                value = 42
            }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, comparisonExpression, functionDeclaration, functionalType, ifExpression, inline,
integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, thisExpression, whileLoop */
