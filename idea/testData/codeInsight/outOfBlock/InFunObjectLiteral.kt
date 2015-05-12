// FALSE
interface Some

fun test() {
    object : Some {
        fun test(<caret>) {

        }
    }
}