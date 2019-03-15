// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: bar$default
package test

inline fun bar(f: () -> String = { "OK" }) = f()
// FILE: 2.kt

import test.*
// SKIP_INLINE_CHECK_IN: foo$default
inline fun foo(f: () -> String = { bar() }) = f()

fun box(): String {
    return foo()
}

