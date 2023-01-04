// FIR_IDENTICAL
// !DIAGNOSTICS: -NOTHING_TO_INLINE

<!INLINE_EXTERNAL_DECLARATION!>inline external fun foo(): Unit<!>

inline external val bar: Int
    <!INLINE_EXTERNAL_DECLARATION!>get()<!> = definedExternally

external val baz: Int
    <!INLINE_EXTERNAL_DECLARATION!>inline get()<!> = definedExternally

external class A {
    <!INLINE_EXTERNAL_DECLARATION!>inline fun foo(): Unit<!>

    inline val bar: Int
        <!INLINE_EXTERNAL_DECLARATION!>get()<!> = definedExternally

    val baz: Int
        <!INLINE_EXTERNAL_DECLARATION!>inline get()<!> = definedExternally
}