// IS_APPLICABLE: false
// WITH_RUNTIME
annotation class Test {
    companion object {
        @JvmField
        <caret>val foo = 1
    }
}