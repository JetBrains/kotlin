// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a

fun test() {
    val some = if () {
        fun other() {
            <caret>
        }
    }
    else {

    }
}