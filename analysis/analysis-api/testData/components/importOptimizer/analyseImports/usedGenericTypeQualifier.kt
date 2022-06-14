// FILE: main.kt
package test

import dependency.Bar

class MyClass

fun usage() {
    val a = Bar<MyClass>::foo
}

// FILE: dependency.kt
package dependency

class Bar<T> {
    fun foo() {}
}