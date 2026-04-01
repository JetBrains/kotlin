// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
import O.objMember

@RequiresOptIn
annotation class A

@A
object O {
    fun objMember() {}
}

fun test() {
    <!OPT_IN_USAGE_ERROR!>objMember<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, objectDeclaration */
