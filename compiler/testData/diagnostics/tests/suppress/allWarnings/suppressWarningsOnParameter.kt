// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    fun foo(@Suppress("warnings") p: String?? = "" as String) {}
}