fun test() {
    <caret>when (val a = create()) {
        else -> {
            use("")
            foo()
        }
    }
}

fun create(): String = ""

fun use(s: String) {}

fun foo() {}