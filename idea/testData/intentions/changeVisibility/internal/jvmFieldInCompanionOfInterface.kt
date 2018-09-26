// IS_APPLICABLE: false
// WITH_RUNTIME
interface Test {
    companion object {
        @JvmField
        <caret>val foo = 1
    }
}