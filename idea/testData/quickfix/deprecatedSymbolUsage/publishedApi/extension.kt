// "Replace with generated @PublishedApi bridge call '`access$test`(...)'" "true"
annotation class Z

open class ABase {
    @Z
    protected fun String.test(p: Int) {
    }


    inline fun test() {
        {
            "123".<caret>test(1)
        }()
    }
}