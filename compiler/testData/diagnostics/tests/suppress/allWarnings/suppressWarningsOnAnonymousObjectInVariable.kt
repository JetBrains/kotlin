// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Suppress("warnings")
val anonymous = object {
    fun foo(p: String?? = "" as String) {}
}