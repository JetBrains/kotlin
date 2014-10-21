// FALSE
fun test() {
    val some = if () {
        fun other() {
            <caret>
        }
    }
    else {

    }
}