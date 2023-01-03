// !DIAGNOSTICS: -NOTHING_TO_INLINE

inline external fun foo(): Unit

inline external val bar: Int
    get() = definedExternally

external val baz: Int
    inline get() = definedExternally

external class A {
    inline fun foo(): Unit

    inline val bar: Int
        get() = definedExternally

    val baz: Int
        inline get() = definedExternally
}
