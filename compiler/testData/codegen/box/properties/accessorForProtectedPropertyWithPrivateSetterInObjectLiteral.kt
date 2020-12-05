// FILE: accessorForProtectedPropertyWithPrivateSetterInObjectLiteral.kt
import a.A

class B : A() {
    fun test() = object {
        override fun toString() = vo + fk()()
    }
}

fun box() = B().test().toString()

// FILE: a.kt
package a

open class A {
    protected var vo = "O"
        private set

    protected var vk = ""
        private set

    fun fk() = { ->
        vk = "K"
        vk
    }
}
