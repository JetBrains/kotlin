// "Replace with generated @PublishedApi bridge call '`access$test`(...)'" "true"
annotation class Z

open class ABase {
    @Z
    protected fun <T> test(p: T): T {
        null!!
    }


    inline fun test() {
        {
            //TODO remove generic
            <caret>test<String>("123")
        }()
    }
}