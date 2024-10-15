// RUN_PIPELINE_TILL: BACKEND
fun main() {
    "".run {
        ""
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
