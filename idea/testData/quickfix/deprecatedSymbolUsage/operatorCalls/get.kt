// "Replace with 'get2(i)'" "true"
interface T {
    @Deprecated("", replaceWith = ReplaceWith("get2(i)"))
    operator fun get(i: Int): String

    fun get2(i: Int): String
}

fun test(t: T) {
    val s = <caret>t[0]
}