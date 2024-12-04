class C {
    inline fun <reified T> foo(x: Any): T = x as T

    inline val <reified T> bar: T?
        get() = null as T?
        set(value) {}

    var <reified T> T.x: String
        inline get() = toString()
        inline set(value) {}
}

// COMPILATION_ERRORS