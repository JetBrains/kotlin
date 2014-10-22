// FALSE
trait Some

fun test() {
    object : Some {
        fun test(<caret>) {

        }
    }
}