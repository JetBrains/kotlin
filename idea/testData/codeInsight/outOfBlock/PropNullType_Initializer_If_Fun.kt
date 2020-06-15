// TODO NOTE: [VD] Temporary workaround for KT-36460
// SKIP_ANALYZE_CHECK
// OUT_OF_CODE_BLOCK: TRUE

// ERROR: Expected a value of type Int?
// ERROR: This variable must either have a type annotation or be initialized
// ERROR: Type mismatch: inferred type is Unit but Int? was expected

val test: Int? = if (true) {
    fun test() {
        val t<caret>
    }
}
else {

}