// IS_APPLICABLE: false
open class AA {
    <caret>fun bar() {
    }
    inline fun foo() {
        val result = bar()
    }
}