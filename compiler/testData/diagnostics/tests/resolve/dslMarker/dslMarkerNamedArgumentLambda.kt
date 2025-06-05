// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77648

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class MyMarker

fun annotatedFunctionType(block: @MyMarker String.() -> Unit) {}
fun annotatedFunctionType2(block: @MyMarker Int.() -> Unit) {}

fun String.stringExtension() {}

fun test2() {
    annotatedFunctionType(block = {
        annotatedFunctionType2(block = {
            <!DSL_SCOPE_VIOLATION!>stringExtension<!>()
        })
    })
}


fun annotatedFunctionTypeVararg(vararg block: @MyMarker String.() -> Unit) {}
fun annotatedFunctionTypeVararg2(vararg block: @MyMarker Int.() -> Unit) {}

fun test3() {
    annotatedFunctionTypeVararg(
        block = arrayOf(
            {
                annotatedFunctionTypeVararg2(
                    block = arrayOf(
                        {
                            <!DSL_SCOPE_VIOLATION!>stringExtension<!>()
                        })
                )
            })
    )
}
