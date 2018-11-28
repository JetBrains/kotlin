package inlineProperty

inline val String.foo: Boolean
    get() {
        return true
    }