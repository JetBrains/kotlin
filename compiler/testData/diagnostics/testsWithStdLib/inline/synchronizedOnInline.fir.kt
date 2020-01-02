// !DIAGNOSTICS: -UNUSED_PARAMETER

@Synchronized
inline fun foo(f: () -> Unit): Unit = f()

var bar: String
    @Synchronized
    inline get() = ""
    @Synchronized
    inline set(value) {}

inline var baz: String
    @Synchronized
    get() = ""
    @Synchronized
    set(value) {}
