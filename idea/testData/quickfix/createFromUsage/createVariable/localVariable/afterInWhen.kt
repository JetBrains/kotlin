// "Create local variable 'foo'" "true"
// ERROR: Variable 'foo' must be initialized

fun test(n: Int): Int {
    return when (n) {
        1 -> {
            val foo: Int

            foo
        }
        else -> {
            n + 1
        }
    }
}