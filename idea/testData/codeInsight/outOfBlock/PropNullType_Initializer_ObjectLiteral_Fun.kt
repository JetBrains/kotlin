// TODO NOTE: [VD] Temporary workaround for KT-36460
// SKIP_ANALYZE_CHECK
// OUT_OF_CODE_BLOCK: TRUE

// Problem with lazy initialization of nullable properties

// ERROR: Unresolved reference: a
interface Some

val test: Some? = object: Some {
    fun test() {
        <caret>
    }
}