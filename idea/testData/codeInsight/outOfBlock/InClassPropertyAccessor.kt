// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a
class Test {
    val more : Int = 0
    val test : Int
        get() {
            <caret>
            return more
        }
}