// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun <T: Any> get(t: T): () -> String {
    return t::toString
}
