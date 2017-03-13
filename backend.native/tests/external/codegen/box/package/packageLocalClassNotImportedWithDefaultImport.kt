// FILE: box.kt

package a

import pack.*

class X : SomeClass()

fun box(): String {
    X()
    return "OK"
}

// FILE: file1.kt

package kotlin.jvm

private class SomeClass

// FILE: file2.kt

package pack

public open class SomeClass
