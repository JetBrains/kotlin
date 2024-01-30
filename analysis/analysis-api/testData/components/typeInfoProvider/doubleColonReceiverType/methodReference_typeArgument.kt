// FILE: MyInterface.kt

package test.pkg

interface MyInterface<T> {
    fun sam(): T
}

// FILE: main.kt

import test.pkg.MyInterface

fun test() = MyInterface<String>:<caret>:sam
