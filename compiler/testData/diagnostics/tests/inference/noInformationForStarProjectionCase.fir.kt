// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56134
// WITH_STDLIB

class X {
    fun foo(ls: List<*>) {}
}

fun main() {
    val x = X().foo(
        <!CANNOT_INFER_PARAMETER_TYPE!>mutableListOf<!>()
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, starProjection */
