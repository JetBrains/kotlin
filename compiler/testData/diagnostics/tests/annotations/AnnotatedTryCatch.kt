annotation class My

fun foo(arg: Int): Int {
    try {
        return 1 / (arg - arg)
    } catch (e: @My Exception) {
        return -1
    }
}
