// "Replace with generated @PublishedApi bridge call '`access$test`(...)'" "true"

open class ABase {
    protected fun test(p: Int) {
    }


    inline fun test() {
        {
            <caret>test(1)
        }()
    }

    @PublishedApi
    internal fun `access$test`(p: Int) = test(p)
}