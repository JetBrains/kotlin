// WITH_RUNTIME
fun test() {
    val x = when (val a = create()) {
        else -> {
            use(a, a)
        }
    }<caret>
}

fun create(): String = ""

fun use(s: String, t: String) {}