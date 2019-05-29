fun test() {
    <caret>when (val a = 42) {
        else -> {
            use("")
            foo()
        }
    }
}

fun use(s: String) {}

fun foo() {}