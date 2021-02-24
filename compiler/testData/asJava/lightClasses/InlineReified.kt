// a.InlineReified
package a

class InlineReified {
    inline fun <reified T> foo(x: Any): T = x as T

    inline val <reified T> T.bar: T?
        get() = null as T?

    var <reified T> T.x: String
        inline get() = toString()
        inline set(value) {}
}

// FIR_COMPARISON