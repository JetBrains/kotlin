package test

inline fun call(s: () -> String): String {
    return s()
}

open class Base {

    protected open fun method(): String = "O"

    protected open val prop = "K"
}
