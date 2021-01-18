// SKIP_KT_DUMP
// FILE: kt19306_test1.kt
package test1

abstract class A {
    protected var p = ""
        private set
}

// FILE: kt19306_test2.kt
package test2

import test1.A

class B : A() {
    fun test() = { -> p }
}