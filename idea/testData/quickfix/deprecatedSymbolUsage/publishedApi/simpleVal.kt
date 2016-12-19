// "Replace with generated @PublishedApi bridge call '`access$prop`'" "true"
annotation class Z

open class ABase {
    @Z
    protected val prop = 1

    inline fun test() {
        {
            <caret>prop
        }()
    }
}