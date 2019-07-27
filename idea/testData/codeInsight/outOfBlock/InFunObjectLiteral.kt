// OUT_OF_CODE_BLOCK: FALSE
interface Some

fun test() {
    object : Some {
        fun test(<caret>) {

        }
    }
}