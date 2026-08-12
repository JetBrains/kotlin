// MODULE: lib
// HEADER_MODE
// FILE: lib.kt

package def

interface MyInterface {
    fun foo(): String
}

open class MyDelegate : MyInterface {
    override fun foo(): String = "OK"
}

open class MyBase(delegate: MyInterface) : MyInterface by delegate

// MODULE: main(lib)
// FILE: main.kt

import def.*

class MySub(delegate: MyInterface) : MyBase(delegate)

fun box(): String {
    return "OK"
}
