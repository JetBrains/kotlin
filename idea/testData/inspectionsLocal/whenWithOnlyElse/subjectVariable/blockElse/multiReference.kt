fun test() {
    <caret>when (val a = create()) {
        else -> {
            use(a, a)
            foo()
        }
    }
}

fun create(): String = ""

fun use(s: String, t: String) {}

fun foo() {}