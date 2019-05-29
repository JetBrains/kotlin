// WITH_RUNTIME
fun test() {
    val x = <caret>when (val a = create()) {
        else -> {
            use(a, a)
        }
    }
}

fun create(): String = ""

fun use(s: String, t: String) {}