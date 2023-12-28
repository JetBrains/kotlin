// FILE: accessorForProtectedPropertyWithPrivateSetterWithIntermediateClass.kt
import a.A

open class A2 : A()

class B : A2() {
    fun test() = { -> vo + fk()() }
}

fun box() = B().test()()

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
