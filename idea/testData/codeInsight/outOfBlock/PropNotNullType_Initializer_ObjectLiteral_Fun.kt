// OUT_OF_CODE_BLOCK: FALSE

interface Some

val test: Some = object: Some {
    fun test() {
        <caret>
    }
}