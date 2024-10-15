// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class AnonymousInitializers(var a: String) {
    init {
        a = "s"
    }
}