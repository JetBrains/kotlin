fun test() {
    when (val a = create()) {
        else -> {
            use(a, a)
            foo()
        }
    }<caret>
}

fun create(): String = ""

fun use(s: String, t: String) {}

fun foo() {}