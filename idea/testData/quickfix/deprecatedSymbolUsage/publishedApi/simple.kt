// "Replace with generated @PublishedApi bridge call '`access$test`(...)'" "true"
annotation class Z

open class ABase {
    @Z
    protected fun test(p: Int) {
    }


    inline fun test() {
        {
            <caret>test(1)
        }()
    }
}