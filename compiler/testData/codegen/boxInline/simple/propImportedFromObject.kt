// WITH_RUNTIME
// FILE: 1.kt

package test

object Host {

    inline val <reified T : Any>  T.foo: String
        get() = T::class.java.simpleName
}

// FILE: 2.kt

import test.Host.foo

class OK

fun box(): String {
    return OK().foo
}
