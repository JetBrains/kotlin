fun test() {
    when (val a = create()) {
        else -> {
            use("")
            foo()
        }
    }<caret>
}

fun create(): String = ""

fun use(s: String) {}

fun foo() {}