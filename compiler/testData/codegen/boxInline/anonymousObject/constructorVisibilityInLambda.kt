// FILE: 1.kt

package test

internal class A {
    inline fun doSomething(s: String): String  {
        return {
            s
        }.let { it() }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return A().doSomething("OK")
}
