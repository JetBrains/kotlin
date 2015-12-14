package test

inline fun call(crossinline s: () -> String): String {
    return {
        s()
    }()
}

open class Base {

    protected open fun method(): String = "O"

    protected open val prop = "K"
}
