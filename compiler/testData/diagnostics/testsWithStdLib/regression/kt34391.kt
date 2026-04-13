// RUN_PIPELINE_TILL: FRONTEND

fun main() {
    val list = listOf(A())
    list.forEach(A::<!OPT_IN_USAGE_ERROR!>foo<!>)
    list.forEach {
        it.<!OPT_IN_USAGE_ERROR!>foo<!>()
    }
}

class A {
    @ExperimentalTime
    fun foo() {
        println("a")
    }
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class ExperimentalTime

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, functionDeclaration, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */
