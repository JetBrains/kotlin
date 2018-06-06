// "Replace with 'useSimple(simple(init))'" "true"

fun simple(param: Int = 0, init: () -> Unit): Int {
    init()
    return param
}

@Deprecated("Use useSimple instead", ReplaceWith("useSimple(simple(init))"))
fun use(init: () -> Unit): Int {
    init()
    return 0
}

fun useSimple(s: Int): Int {
    return s
}

fun println(s: String) {}

fun useIt() {
    val t = use<caret> {
        println("abc")
    }
}