// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class Test {
    fun bar() = 1

    fun test(x: Int) {
        val foo: () -> Int = when (x) {
            1 -> {
                <caret>this::bar
            }
            else -> {
                this::bar
            }
        }
    }
}
