// RUN_PIPELINE_TILL: BACKEND
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
