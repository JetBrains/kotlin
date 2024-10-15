// RUN_PIPELINE_TILL: BACKEND
fun main() {
    "".run {
        <!UNUSED_EXPRESSION!>""<!>
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
