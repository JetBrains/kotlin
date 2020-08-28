// "Replace usages of 'set(Int, Int): Unit' in whole project" "true"
interface T {
    @Deprecated("", replaceWith = ReplaceWith("set2(i, v)"))
    operator fun set(i: Int, v: Int)

    fun set2(i: Int, v: Int)
}

fun test(t: T) {
    <caret>t[0] = 1
    t[1] = 2
}