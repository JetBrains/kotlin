// TODO: split SKIP_INLINE_CHECK_IN by files
// SKIP_INLINE_CHECK_IN: bar$default, foo$default
// FILE: 1.kt
package test

inline fun bar(f: () -> String = { "OK" }) = f()
// FILE: 2.kt

import test.*
inline fun foo(f: () -> String = { bar() }) = f()

fun box(): String {
    return foo()
}

