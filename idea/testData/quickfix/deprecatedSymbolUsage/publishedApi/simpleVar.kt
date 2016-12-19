// "Replace with generated @PublishedApi bridge call '`access$prop`'" "true"
annotation class Z

open class ABase {
    @Z
    protected var prop = 1

    inline fun test() {
        {
            <caret>prop
        }()
    }
}