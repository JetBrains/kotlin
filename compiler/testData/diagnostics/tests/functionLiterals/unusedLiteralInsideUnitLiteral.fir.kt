// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION

fun main() {
    "".run {
        {}
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
