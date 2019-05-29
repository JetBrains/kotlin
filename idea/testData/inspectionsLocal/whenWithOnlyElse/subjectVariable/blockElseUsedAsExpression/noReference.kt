// WITH_RUNTIME
fun test() {
    val x = <caret>when (val a = create()) {
        else -> {
            use("")
        }
    }
}

fun create(): String = ""

fun use(s: String) {}