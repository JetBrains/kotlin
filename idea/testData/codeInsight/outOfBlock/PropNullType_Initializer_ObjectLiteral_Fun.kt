// OUT_OF_CODE_BLOCK: FALSE
// Problem with lazy initialization of nullable properties

// TODO: Investigate
// --ERROR: Unresolved reference: q
interface Some

val test: Some? = object: Some {
    fun test() {
        <caret>
    }
}