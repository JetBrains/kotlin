// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
// LANGUAGE_VERSION: 1.2
// SKIP_INLINE_CHECK_IN: inlineFun$default
//WITH_RUNTIME
package test

class OK
class FAIL

inline fun <reified T> inlineFun(lambda: () -> String = { T::class.java.simpleName }): String {
    return lambda()
}

inline fun <reified X> inlineFun2(): String {
    return inlineFun<X>()
}

// FILE: 2.kt

import test.*



fun box(): String {
    return inlineFun2<OK>()
}
