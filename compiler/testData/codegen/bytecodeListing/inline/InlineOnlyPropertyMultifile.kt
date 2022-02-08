// WITH_STDLIB

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    prop
}

// No method should be generated in multifile facade for 'inlineOnly'
// Because 'inlineOnly' is private in file part (because it's inline-only) and can't be delegated from facade
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline var prop: String
    get() = "12"
    set(value) {}

inline var prop2: String
    get() = "12"
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    set(value) {}
