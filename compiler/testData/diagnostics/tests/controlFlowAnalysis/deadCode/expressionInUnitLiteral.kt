fun main(args: Array<String>) {
    "".run {
        <!UNUSED_EXPRESSION!>""<!>
    }
}


fun <T> T.run(f: (T) -> Unit): Unit = f(this)
