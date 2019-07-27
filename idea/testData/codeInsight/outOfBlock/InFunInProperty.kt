// OUT_OF_CODE_BLOCK: FALSE
fun test() {
    val some = if () {
        fun other() {
            <caret>
        }
    }
    else {

    }
}