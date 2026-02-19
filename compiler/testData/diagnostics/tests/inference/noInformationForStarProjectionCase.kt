// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56134
// WITH_STDLIB

class X {
    fun foo(ls: List<*>) {}
}

fun main() {
    val x = X().foo(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>mutableListOf<!>()
    )
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, starProjection */
