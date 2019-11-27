// OUT_OF_CODE_BLOCK: FALSE
// ERROR: A type annotation is required on a value parameter

interface Some

fun test() {
    object : Some {
        fun test(<caret>) {

        }
    }
}