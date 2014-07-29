fun main(args: Array<String>) {
    "".run {
        <!UNUSED_FUNCTION_LITERAL!>{}<!>
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
