// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a

interface Some

val test: Some = object: Some {
    fun test() {
        <caret>
    }
}