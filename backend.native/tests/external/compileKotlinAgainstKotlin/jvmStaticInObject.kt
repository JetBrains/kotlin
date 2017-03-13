// IGNORE_BACKEND: NATIVE
// FILE: A.kt

package aaa

import kotlin.jvm.*

public object TestObject {
    @JvmStatic
    public val test: String = "OK"
}

// FILE: B.kt

fun box(): String {
    return aaa.TestObject.test
}
