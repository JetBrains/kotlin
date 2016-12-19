// "Replace with generated @PublishedApi bridge call '`access$prop`'" "true"
annotation class Z

open class ABase {
    @Z
    protected var String.prop: Int
        get() = 1
        set(field) {}


    inline fun test() {
        {
            "123".<caret>prop
        }()
    }
}