// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

package aaa

import kotlin.jvm.*

public object TestObject {
    @JvmStatic
    public val test: String = "OK"
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    return aaa.TestObject.test
}
