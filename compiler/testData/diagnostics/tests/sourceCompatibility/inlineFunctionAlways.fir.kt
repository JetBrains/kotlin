// !DIAGNOSTICS: -NOTHING_TO_INLINE
// !LANGUAGE: -InlineProperties

inline fun String.test() {
}

inline fun test() {
}

class A {
    inline fun String.test() {
    }

    inline fun test() {
    }
}