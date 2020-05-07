// FILE: annotations.kt

package allopen

annotation class Open

// FILE: main.kt

import allopen.Open

@Open
class A {
    fun foo() {

    }
}

@Open
class B : A() {
    override fun foo() {

    }
}