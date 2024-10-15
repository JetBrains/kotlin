// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    @Suppress("warnings")
    companion object {
        val foo: String?? = null as Nothing?
    }
}