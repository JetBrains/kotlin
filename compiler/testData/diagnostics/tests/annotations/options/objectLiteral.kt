// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Target(AnnotationTarget.CLASS)
annotation class Ann

open class My

fun foo(): My {
    return (@Ann object: My() {})
}