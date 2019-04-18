fun test() {
    when (val a = 42) {
        else -> {
            use("")
            foo()
        }
    }<caret>
}

fun use(s: String) {}

fun foo() {}