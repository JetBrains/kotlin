// OUT_OF_CODE_BLOCK: FALSE
// ERROR: This variable must either have a type annotation or be initialized
// ERROR: Type mismatch: inferred type is Unit but Int? was expected
// ERROR: Type mismatch: inferred type is Unit but Int? was expected

val test: Int? = if (true) {
    fun test() {
        val t<caret>
    }
}
else {

}