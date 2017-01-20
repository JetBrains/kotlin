@Suppress("NOTHING_TO_INLINE")
inline fun foo(vararg args: Int) {
    for (a in args) {
        println(a.toString())
    }
}

fun bar() {
    foo(1, 2, 3)
}

fun main(args: Array<String>) {
    bar()
}
