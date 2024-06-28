// DIAGNOSTICS: +UNUSED_LAMBDA_EXPRESSION

fun main() {
    "".run {
        <!UNUSED_LAMBDA_EXPRESSION!>{}<!>
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
