// RUN_PIPELINE_TILL: FRONTEND
@RequiresOptIn
annotation class A

@A
open class C {
    fun member() {}
}

fun foo(f: <!OPT_IN_USAGE_ERROR!>C<!>.() -> Unit) {}


fun test() {
    <!OPT_IN_USAGE_ERROR!>foo<!> {
        <!OPT_IN_USAGE_ERROR!>member<!>()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, functionalType, lambdaLiteral,
typeWithExtension */
